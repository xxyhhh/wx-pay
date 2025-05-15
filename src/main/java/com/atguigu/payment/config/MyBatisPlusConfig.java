package com.atguigu.payment.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.atguigu.payment.mapper")
public class MyBatisPlusConfig {


}
