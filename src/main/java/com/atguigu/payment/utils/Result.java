package com.atguigu.payment.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;

@Data
@Accessors(chain = true)
public class Result<T> {
    private Integer code;  //状态码
    private String message; //返回信息
    private T data;

    private Result() {
    }

    //返回数据
    public static <T> Result<T> build(T data) {
        Result<T> Result = new Result<>();
        if (data != null) {
            Result.setData(data);
        }
        return Result;
    }

    public static <T> Result<T> build(T data, ResultCodeEnum resultCodeEnum) {
        Result<T> Result = build(data);
        Result.setCode(resultCodeEnum.getCode());
        Result.setMessage(resultCodeEnum.getMessage());
        return Result;
    }
    public static <T> Result<T> build(T data,Integer code, String message) {
        Result<T> Result = build(data);
        Result.setCode(code);
        Result.setMessage(message);
        return Result;
    }

    //返回结果
    public static <T> Result<T> ok(T data) {
        return Result.build(data, ResultCodeEnum.SUCCESS);
    }

    public static <T> Result<T> ok(T data, String message) {
        return Result.build(data, ResultCodeEnum.SUCCESS.getCode(), message);
    }

    public static Result ok(String key, Object value) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(key, value);
        return Result.ok(map);
    }

    public static <T> Result<T> fail(T data) {
        return Result.build(data, ResultCodeEnum.ERROR);
    }

    public static <T> Result<T> fail(T data, String message) {
        return Result.build(data, ResultCodeEnum.ERROR.getCode(), message);
    }
}
