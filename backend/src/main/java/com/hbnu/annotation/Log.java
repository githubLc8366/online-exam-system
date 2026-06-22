package com.hbnu.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String module() default "";   // 操作模块
    String action() default "";   // 操作动作
}