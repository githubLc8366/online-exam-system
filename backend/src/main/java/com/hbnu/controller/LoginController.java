package com.hbnu.controller;

import com.hbnu.common.Result;
import com.hbnu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录控制器
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录接口
     *
     * @param params 包含 userNo 和 password
     * @return Result 包含 token 和用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String userNo = params.get("userNo");
        String password = params.get("password");
        String role = params.get("role");  // 新增

        if (userNo == null || userNo.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        return userService.login(userNo, password, role);  // 传入role
    }
}