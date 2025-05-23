package com.atguigu.payment.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.http.HttpClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import com.wechat.pay.java.core.util.PemUtil;

import java.security.PrivateKey;


@Configuration
@PropertySource("classpath:wxpay.properties") //读取配置文件
@ConfigurationProperties(prefix = "wxpay") //读取wxpay节点
@Data //使用set方法将wxpay节点中的值填充到当前类的属性中
public class WxPayConfig {

    // 商户号
    private String mchId;

    // 商户API证书序列号
    private String mchSerialNo;

    // 商户私钥文件
    private String privateKeyPath;

    // APIv3密钥
    private String apiV3Key;

    // 商户APPID
    private String appid;

    // 微信服务器地址
    private String domain;

    // 接收结果通知地址(回调地址)
    private String notifyDomain;

    // 全局复用 Config 实例，避免重复创建
    private static Config config;

    /**
     * 获取商户的私钥
     * @return
     */
    public PrivateKey getPrivateKey() {
        try {
            return PemUtil.loadPrivateKeyFromPath(privateKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("私钥文件不存在",e);
        }
    }

    /**
     * 获取并自动更新微信平台证书，具有以下功能。
     * 1、首次构建时下载证书,
     * 2、后台线程定时更新（每60min），
     * 3、证书过期平滑切换(使用新的证书后旧证书仍保留一段时间，确保服务无中断)
     * 4、若首次下载失败则抛出异常；后续更新失败会记录日志但不会中断业务
     *
     * 内部用的是 AutoCertificateService  ---> 接口是Config
     * @return
     */
    @Bean
    public RSAAutoCertificateConfig getConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(mchSerialNo)
                .apiV3Key(apiV3Key)
                .build();
    }
}
