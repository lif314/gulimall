package com.lif314.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.lif314.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000119642680";

    // 商户私钥，您的PKCS8格式RSA2私钥 -- 应用私钥
    // 私钥自己加密
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCL+k8WVN0gTwvjvNaff5UDozutbBVZA2AWdVplS0Yz1eXfUX1FEmB6YSuOu+pUl8HMl0/bk1EET0CBWOLmHBRgkbLrvEzyaKgu/J2Pp3JGgkREgpDA1a2F9lPQ8dcJqgF3EwmtQ8NBGI0CnO6L0l734Y7ZP6iIXFq+K/kFSFNxFtNg3StEDj5+xxWxjHjeLW6EzFyubKRc2sav6zCwP7XI0emVaKcRIFKrEdRB+TJ6Emi9E7+/bYPerD1Zt7M7YzvCZbTLZJ3GxMe7QlVZYOjTmyUnDM+pa6F+jHQLZ0y413w5bA7n8GHqeCzygfV7YjzN8x5YamONgDomHxxj6eDlAgMBAAECggEAdp6CwMjfro5t4+rV8cnbDH8ahmbuXEVI+x8toGM+tZSQvUNAJfVhvrNzhvxlopQLzgV3zfo0ELPcVQBvH1MyTXeKqMwkZNQdmdvG5cKfS3L+yRPf+Rnad4h8FLesY+smXLLMY4DmCNb2P/2fBOwcQHFrbVzNw+iHmVqIJ1rYIx9PlbPkW4x7IPDYTMItkWScBqcx9gIC2pkIptx3E+q8yF9lT2y520RVJ0k87WxXlROf8KVufZykf5WgbsKTMQvP4mG7pkAPAMSc374G2VKIFnpN3E8VQ2jrJcGxbZWSuT2fGde8i2uCbI6qtwQnpJ4G7QXhkNuPkrbVkrqWhKxcbQKBgQDi9oTGpOmJvVhAlEBiG1ce1RqgVa/O2J5AhR3+bNe2SVQSRAM+Z50NQDYbETa9XAxNYCVNG5rVdsAf+MxB0rXvL53eN4lLq9HQkuHGrtAosTKz64ktAVPHqvBLcIpIE+WVA5Sc/nWkNv8NxC/mXUS+M+YamUofTMU5tpXTGES08wKBgQCd4traGltsPIMrLSbetzYJIHq+5j3M5gTj8bPgXSWSQ+c50L+R9FasA4GJagXNhrzhMaj0KcaMZ6nig1NEvCfbnZ6Rn6BQFkuTh7UgM5+soHVpeMekUq/QzEpwmZ05rYyidiU5fGYo/zwv4fy3CiOUy+UJufLuB7fChufo6KfoxwKBgA+dgGMeY4b7hP/kc02MrgDMDqnrW04y7yhnQDoKCQlcoDElhsebX13TBiX0mDyNAbetHsPgW1XGds98UalRsvzC3Oy2C3cuWiAsiuYdjurNzjw8v1JeXgJFy4SVOJ5e6BPJjEcE5tkmg4PR9K23ywv/DBzWYRgoMwWWf1ZnvAprAoGABlfb5RFUBPg1aOGqgx4nPJyicdL8PqQrJCFM0cHMRaEWxBrf53RCmyyU4rlwvD4ijoWZPR4EjRWJHMGb8dIBY/BkM4OPREhxBt9X5pBMZPoZXYBtvOtZhvD+OKHjUDLpyIkom7OkwxbmCyJwLcZpgYvjis1+thN3TYhv/sqCHHcCgYEArKNM1CsRjecx6mUGVBn3oTpqXzlBm8Rd/3RHGstGnYi+t/Sd68ikUpT0ZXBhpR+cCPQ7dpte8J2CRVvKOtjpryw5+uU+gaifKSMlWBfHO2E6UUGx5k25k0rX/YUgTCZi4mm+dF9Is4SiwngDDyvdQtioS8zshg1RavUe9lsB+EI=";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiLxAWn3YfthrljmeY/UxphqGLSFEGaObRt0CzJr9jtH+YhaYqwcNiLnD0g+OI3nL7pKoJRiUNkNmDVHo50lFczjRjIBez3GkiG3eqHZc1OP5QXBSW2WmokLpwCmFLx33bQ1NqwUOY06lreKlEDnFnuJQ6rsYBlpR6XK+jPffzCKHsFEWkv1LRUvIES4CBCZg60XfNYTwC/MEKCaucRESwcbthxAyo1+UcQ+1ZcmA8cxuJpYcIEyovQ3/BJFnTJX9aU8AZV+2r+8k7f+nFxOhBLKTNXl2FCWjykDuyIsa2SO+08s0JJk9hmiLMswpbQ0/UldVLyNeD/SyCxXMPHTOGwIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url;

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        // 会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
