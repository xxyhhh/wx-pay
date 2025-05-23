package com.atguigu.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ClassName:
 * Package: com.atguigu.payment
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/12 11:02
 * @Version 1.0
 */
@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
