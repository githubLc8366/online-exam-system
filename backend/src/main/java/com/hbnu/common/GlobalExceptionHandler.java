package com.hbnu.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理业务异常
   */
  @ExceptionHandler(BusinessException.class)
  public Result<?> handleBusinessException(BusinessException e) {
    // 记录异常日志
    System.err.println("业务异常: " + e.getMessage());

    // 返回业务错误信息
    return Result.error(e.getMessage());
  }

  /**
   * 处理所有其他异常
   */
  @ExceptionHandler(Exception.class)
  public Result<?> handleException(Exception e) {
    // 记录异常日志
    System.err.println("全局异常捕获: " + e.getMessage());
    e.printStackTrace();

    // 返回统一错误格式
    return Result.error("系统内部错误，请稍后重试");
  }
}
