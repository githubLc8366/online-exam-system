package com.hbnu.controller;

import com.hbnu.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统健康检查控制器
 */
@RestController
@RequestMapping("/api")
public class SystemController {

    /**
     * 健康检查接口
     * 用于验证后端服务是否正常运行
     * 
     * @return 通用返回结果
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Backend service is running!");
    }
}