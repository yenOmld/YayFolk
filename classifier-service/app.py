from pathlib import Path
import os

import torch
from flask import Flask, jsonify, request
from PIL import Image
from torchvision import transforms
from torchvision.models import mobilenet_v2

APP_ROOT = Path(__file__).resolve().parent
DEFAULT_CHECKPOINT = Path(r"D:/Cursor/program/pic/outputs/mobilenetv2_best.pth")
CHECKPOINT_PATH = Path(os.getenv("YAYFOLK_CLASSIFIER_CHECKPOINT", str(DEFAULT_CHECKPOINT))).resolve()
HOST = os.getenv("YAYFOLK_CLASSIFIER_HOST", "127.0.0.1")
PORT = int(os.getenv("YAYFOLK_CLASSIFIER_PORT", "5001"))
DEFAULT_TOPK = int(os.getenv("YAYFOLK_CLASSIFIER_TOPK", "3"))
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

app = Flask(__name__)

model = None
class_names = []
transform = None
image_size = 224


def load_checkpoint(checkpoint_path: Path):
    if not checkpoint_path.is_file():
        raise FileNotFoundError(f"Checkpoint not found: {checkpoint_path}")
    return torch.load(str(checkpoint_path), map_location=DEVICE)


def build_model(num_classes: int, dropout: float):
    net = mobilenet_v2(weights=None)
    in_features = net.classifier[1].in_features
    net.classifier = torch.nn.Sequential(
        torch.nn.Dropout(p=dropout),
        torch.nn.Linear(in_features, num_classes),
    )
    return net


def init_model():
    global model, class_names, transform, image_size

    checkpoint = load_checkpoint(CHECKPOINT_PATH)
    class_names = checkpoint["class_names"]
    image_size = checkpoint.get("image_size", 224)
    dropout = checkpoint.get("dropout", 0.2)
    mean = checkpoint.get("normalize_mean", [0.485, 0.456, 0.406])
    std = checkpoint.get("normalize_std", [0.229, 0.224, 0.225])

    model = build_model(num_classes=len(class_names), dropout=dropout)
    model.load_state_dict(checkpoint["model_state_dict"])
    model.to(DEVICE)
    model.eval()

    transform = transforms.Compose(
        [
            transforms.Resize(int(image_size * 1.14)),
            transforms.CenterCrop(image_size),
            transforms.ToTensor(),
            transforms.Normalize(mean=mean, std=std),
        ]
    )


def parse_topk(raw_value):
    try:
        return max(1, int(raw_value))
    except (TypeError, ValueError):
        return DEFAULT_TOPK


@app.get("/health")
def health():
    return jsonify(
        {
            "status": "ok",
            "modelLoaded": model is not None,
            "checkpointPath": str(CHECKPOINT_PATH),
            "device": str(DEVICE),
            "classCount": len(class_names),
            "defaultTopk": DEFAULT_TOPK,
        }
    )


@app.post("/predict")
def predict():
    if model is None or transform is None:
        return jsonify({"error": "Model is not loaded."}), 500

    image_file = request.files.get("image")
    if image_file is None:
        return jsonify({"error": "Missing multipart field 'image'."}), 400

    if not image_file.filename:
        return jsonify({"error": "Empty filename."}), 400

    topk = min(parse_topk(request.form.get("topk") or request.args.get("topk")), len(class_names))

    try:
        image = Image.open(image_file.stream).convert("RGB")
        tensor = transform(image).unsqueeze(0).to(DEVICE)

        with torch.no_grad():
            logits = model(tensor)
            probabilities = torch.softmax(logits, dim=1)[0]

        values, indices = torch.topk(probabilities, k=topk)
        predictions = []
        for rank, (score, index) in enumerate(zip(values.tolist(), indices.tolist()), start=1):
            predictions.append(
                {
                    "rank": rank,
                    "tag": class_names[index],
                    "confidence": float(score),
                }
            )

        primary = predictions[0]
        return jsonify(
            {
                "success": True,
                "primaryTag": primary["tag"],
                "confidence": primary["confidence"],
                "predictions": predictions,
            }
        )
    except Exception as exc:
        return jsonify({"error": f"Prediction failed: {exc}"}), 500


if __name__ == "__main__":
    init_model()
    print(f"YayFolk classifier service listening on http://{HOST}:{PORT}")
    print(f"Checkpoint: {CHECKPOINT_PATH}")
    print(f"Device: {DEVICE}")
    app.run(host=HOST, port=PORT, debug=False)
