package com.hbnu.common;

import lombok.Data;

/**
 * 通用返回结果类
 * 
 * @param <T> 数据类型
 */
@Data
public class Result<T> {
    
    /** 状态码：200成功，500失败 */
    private Integer code;
    
    /** 提示信息 */
    private String msg;
    
    /** 返回数据 */
    private T data;
    
    /**
     * 私有构造函数
     */
    private Result() {
    }
    
    /**
     * 私有构造函数
     * 
     * @param code 状态码
     * @param msg 提示信息
     * @param data 返回数据
     */
    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    /**
     * 成功返回（带数据）
     * 
     * @param data 返回数据
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    /**
     * 成功返回（带自定义消息）
     * 
     * @param msg 提示信息
     * @param data 返回数据
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }
    
    /**
     * 成功返回（仅消息，无数据）
     * 
     * @param msg 提示信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String msg) {
        return new Result<>(200, msg, null);
    }
    
    /**
     * 失败返回（默认状态码500）
     * 
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
    
    /**
     * 失败返回（自定义状态码）
     * 
     * @param code 状态码
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}