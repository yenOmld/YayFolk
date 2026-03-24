class PCMProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.port.onmessage = (event) => {
      // 处理来自主线程的消息
    };
  }

  process(inputs, outputs, params) {
    const input = inputs[0];
    if (input.length > 0) {
      const channelData = input[0];
      
      // 将Float32转换为Int16
      const int16Data = new Int16Array(channelData.length);
      for (let i = 0; i < channelData.length; i++) {
        const s = Math.max(-1, Math.min(1, channelData[i]));
        int16Data[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
      }
      
      // 发送PCM数据到主线程
      this.port.postMessage({
        type: 'pcmData',
        data: int16Data
      });
    }
    return true;
  }
}

registerProcessor('pcm-processor', PCMProcessor);