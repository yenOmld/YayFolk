package org.example.java_web_01.controller.controllerImpL;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;


import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.example.java_web_01.severice.CartService;
import org.example.java_web_01.severice.ImpL.OrderServiceImpl;
import org.example.java_web_01.severice.OrderService;
import org.example.java_web_01.util.PayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.example.java_web_01.pojo.Result;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api/alipay")
public class AliPayController {
    @Autowired
    private PayUtil payUtil;


    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;


    @ResponseBody
    @PostMapping("/pay")
    public Result alipay(@RequestBody PayRequest payRequest) throws AlipayApiException {

        System.out.println("订单号是" + payRequest);

        //生成订单号（支付宝的要求？）
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String user = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        List<Long> orderIds = payRequest.getOrderIds();
        String payId = time + user;
        for (Long orderId : orderIds) {
            int i = orderId.intValue();
            boolean b = cartService.payForProduct(i, payId);
        }

        float value = Float.parseFloat(payRequest.getValue());
//        //调用封装好的方法（给支付宝接口发送请求）
        String content = payUtil.sendRequestToAlipay(payId, value, "支付数据");

        return Result.success(content);

    }


    

    //    当我们支付完成之后跳转这个请求并携带参数，我们将里面的订单id接收到，通过订单id查询订单信息，信息包括支付是否成功等
    @GetMapping("/toSuccess")
    public String returns(String out_trade_no) throws ParseException {
        System.out.println("订单号是" + out_trade_no);
        // 查询支付宝交易状态
        String query = payUtil.query(out_trade_no);
        System.out.println("==>" + query);

        // 解析支付宝返回的JSON响应数据
        JSONObject jsonObject = JSONObject.parseObject(query);
        Object o = jsonObject.get("alipay_trade_query_response");
        Map map = (Map) o;

        System.out.println(map);
        Object s = map.get("trade_status");
        if (s.equals("TRADE_SUCCESS")) {
            //当支付成功之后要执行的操作
            System.out.println("订单支付成功");
            return "redirect:http://localhost:5173/user/cart";
        } else {
//            支付失败要执行的操作
            System.out.println("订单支付失败");
            return "index";
        }
    }

    /**
     * 退款接口
     *
     * @throws AlipayApiException
     */
    @PostMapping("/refund")
    @ResponseBody
    public Result aliPayRefund(@RequestBody RefundRequest refundRequest) throws AlipayApiException {
        System.out.println("订单号是" + refundRequest);
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(payUtil.getAlipayConfig());

        // 构造请求参数以调用接口
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();

        // 设置商户订单号
        model.setOutTradeNo(refundRequest.getPayId());


        // 设置退款金额
        model.setRefundAmount(refundRequest.getValue());

        // 设置退款原因说明
        model.setRefundReason("正常退款");


        request.setBizModel(model);

        AlipayTradeRefundResponse response = alipayClient.execute(request);
        System.out.println(response.getBody());

        if (response.isSuccess()) {
            System.out.println("调用成功");
            orderService.refundOrder(refundRequest.getOrderId(),refundRequest.getReason());
            return Result.success("退单成功");
        } else {
            System.out.println("调用失败");
            return Result.error("退款失败");
        }
    }

}