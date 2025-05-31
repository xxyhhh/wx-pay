package com.atguigu.payment.config;

import com.alipay.api.*;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * ClassName:
 * Package: com.atguigu.payment.config
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/26 10:37
 * @Version 1.0
 */
@Configuration
@PropertySource("classpath:alipay-sandbox.properties")
public class AlipayClientConfig {

    @Resource
    private Environment config;
    @Bean
    public AlipayClient alipayClient() {
        AlipayConfig alipayConfig = new AlipayConfig();
        //设置网关地址
        alipayConfig.setServerUrl(config.getProperty("alipay.gateway-url"));
        //设置应用APPID
        alipayConfig.setAppId(config.getProperty("alipay.app-id"));
        //设置应用私钥
        alipayConfig.setPrivateKey(config.getProperty("alipay.merchant-private-key"));
        //设置请求格式，固定值json
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(config.getProperty("alipay.alipay-public-key"));
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        //  设置内容加密密钥和密钥类型
//        alipayConfig.setEncryptKey( config.getProperty("alipay.content-key"));
//         alipayConfig.setEncryptType(config.getProperty("alipay.encrypt-type"));
        alipayConfig.setReadTimeout(30000); //沙箱下获取账单url会超时，给他多点时间
        //构造client
        AlipayClient alipayClient = null;
        try {
            alipayClient = new DefaultAlipayClient(alipayConfig);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        return alipayClient;
    }
}
