package org.example.java_web_01.util;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PayUtil {

    //appid
    private final String APP_ID = "9021000162649830";
    //应用私钥
    private final String APP_PRIVATE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj0BY8q8QL3cJ67ZyZ1nWZFPBciKdoLNWW2ftWeUTxTxcGFEsITzZ3rxPCmBtv+tZodBdgLGCOlhy+7K7xfNdaFcFn/uv8G/m3zEt6Na9GuIgaFnGV1QkbmGfxIHUsnipkKtydctv1C3Ah8KYEbTpllBQrFeWZXCJwbcu+3lSE7ByTfgRYVp0ablNd8L2ElBS4KA51zSzdKdiKuZUoZxRYCTKXWyVH75e1t/QWaz87Qf1xXNyWt5z4da/YS1lvWpHpGgKBmxMDwIKzVnlcJpdR1M7MLJ0p6k5E6sH57zCKyWLDoyviRYXYBgvXZnoKfExK5pdSQ7cJ/RtjHGBijApEwIDAQAB";
    //    字符编码
    private final String CHARSET = "UTF-8";
    // 支付宝公钥
    private final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0vAUJRsAaPNwYj5eK7haDNhM6ww73kPxp+ukAxK1NUb2/Q4X+A76yYwxhaVDjhXdqA4HITQDLfjX7JyOAUnM38pnV5WZngCXAv4sgZcZJu5heY8b/mlCx37+0UUwv+6CK3Nm9/olUKEYuGgHlrLvS/JJUp3ozwHZEPhKVVQvHbQkM1+W1vwy33cgQ0SCMtFyNt90KTXjBkX4ayj+NnagZGZLlYHvM7fBzJdnUw06OR/r5FMLdo1UUByYG2GmaRjle+4rZebR29Xa0W2Fscvq5uribYdtY+zX2nTuioU313TgyQKHClfBXJOSqQzjcIfRY5IpPWWU4xryuWaMvc9DwQIDAQAB";
    //网关地址
    private final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private final String FORMAT = "JSON";
    //签名方式
    private final String SIGN_TYPE = "RSA2";
    //支付宝异步通知路径,付款完毕后会异步调用本项目的方法,必须为公网地址
    private final String NOTIFY_URL = "http://qbb2e542.natappfree.cc/api/alipay/toSuccess";
    //支付宝同步通知路径,也就是当付款完毕后跳转本项目的页面,可以不是公网地址
    private final String RETURN_URL = "http://localhost:5173/personal/activities";
    private AlipayClient alipayClient = null;
    //支付宝官方提供的接口
    public String sendRequestToAlipay(String outTradeNo, Float totalAmount, String subject) throws AlipayApiException {
        //获得初始化的AlipayClient
        alipayClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(RETURN_URL);
        alipayRequest.setNotifyUrl(NOTIFY_URL);


        //商品描述（可空）
        String body = "";
        alipayRequest.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + totalAmount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        //请求
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        System.out.println("返回的结果是："+result );
        return result;
    }

    //    通过订单编号查询
    public String query(String id){
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", id);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        String body=null;
        try {
            response = alipayClient.execute(request);
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return body;
    }

    public AlipayConfig getAlipayConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(GATEWAY_URL);
        alipayConfig.setAppId(APP_ID);
        alipayConfig.setPrivateKey(APP_PRIVATE_KEY);
        alipayConfig.setFormat(FORMAT);
        alipayConfig.setAlipayPublicKey(ALIPAY_PUBLIC_KEY);
        alipayConfig.setCharset(CHARSET);
        alipayConfig.setSignType(SIGN_TYPE);
        return alipayConfig;
    }
}
