package com.atguigu.payment;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * ClassName:
 * Package: com.atguigu.payment
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/26 10:38
 * @Version 1.0
 */
@SpringBootTest
@Slf4j
public class AlipayTest {

    @Resource
    private Environment config;

    @Test
    public void test01() {
        log.info("app-id:{}", config.getProperty("alipay.app-id"));
        log.info("seller-id:{}", config.getProperty("alipay.seller-id"));
        log.info("gateway-url:{}", config.getProperty("alipay.gateway-url"));
    }
}
