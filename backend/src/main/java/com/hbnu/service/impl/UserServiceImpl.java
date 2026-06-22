package com.hbnu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.mapper.RoleMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.User;
import com.hbnu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Result<Map<String, Object>> login(String userNo, String password, String role) {
        // 1. 根据账号查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", userNo);
        queryWrapper.eq("is_deleted", 0);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            return Result.error("用户不存在");
        }

        // 2. 验证密码
        String storedPassword = user.getPassword();
        boolean passwordMatch = false;

        if (password.equals(storedPassword)) {
            passwordMatch = true;
        }

        if (!passwordMatch && storedPassword.startsWith("$2a$")) {
            try {
                passwordMatch = passwordEncoder.matches(password, storedPassword);
            } catch (Exception e) {}
        }

        if (!passwordMatch) {
            return Result.error("密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            return Result.error("账号已被锁定");
        }

        // 4. 查询用户所有角色
        List<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            return Result.error("该账号未分配角色，请联系管理员");
        }

        // 5. 校验前端选择的角色
        if (role != null && !role.isEmpty() && !roles.contains(role)) {
            return Result.error("该账号没有【" + getRoleName(role) + "】角色权限");
        }

        // 6. 确定当前角色
        String roleCode = (role != null && !role.isEmpty()) ? role : roles.get(0);

        // 7. 生成 JWT Token
        String token = JwtUtils.generateToken(user.getId(), roleCode);

        // 8. 组装返回数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("userNo", user.getUserNo());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", roleCode);
        userInfo.put("roles", roles);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", userInfo);

        return Result.success("登录成功", data);
    }

    private String getRoleName(String role) {
        switch (role) {
            case "admin": return "管理员";
            case "teacher": return "教师";
            case "student": return "学生";
            default: return role;
        }
    }
}