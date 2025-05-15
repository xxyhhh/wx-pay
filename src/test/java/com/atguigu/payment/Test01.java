package com.atguigu.payment;

import com.atguigu.payment.config.WxPayConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;

/**
 * ClassName:
 * Package: com.atguigu.payment
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/12 12:11
 * @Version 1.0
 */
@SpringBootTest
public class Test01 {

    @Resource
    private WxPayConfig wxPayConfig;

    @Test
    public void test01(){
        PrivateKey privateKey = wxPayConfig.getPrivateKey();
        System.out.println("privateKey = " + privateKey);
    }
}
