package com.hbnu.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")           // 拦截所有 /api/ 请求
                .excludePathPatterns(
                        "/api/login",                  // 登录接口
                        "/api/health",                 // 健康检查
                        "/api/session/list",           // 考试列表（需根据实际情况调整）
                        "/api/session/detail/**"       // 考试详情
                );
    }
}