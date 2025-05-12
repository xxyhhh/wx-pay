package com.atguigu.payment.utils;

import lombok.Getter;

/**
* ClassName: 
* Package: com.itmk.utils
* Description:
* @Author 何鸿辉
* @Create 2024/10/19 18:23
* @Version 1.0
*/
@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "成功"),
    ERROR(500, "失败"),
    NO_LOGIN(600, "未登录"),
    NO_AUTH(700, "未授权");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}