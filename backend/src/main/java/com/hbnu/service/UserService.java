package com.hbnu.service;

import com.hbnu.common.Result;

import java.util.Map;

/**
 * 用户 Service 接口
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param userNo   账号
     * @param password 密码
     * @return Result 包含 token 和用户信息
     */
    Result<Map<String, Object>> login(String userNo, String password, String role);
}