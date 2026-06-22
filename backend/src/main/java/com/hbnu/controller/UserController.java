package com.hbnu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.mapper.ClazzMapper;
import com.hbnu.mapper.DepartmentMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.Clazz;
import com.hbnu.pojo.Department;
import com.hbnu.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 个人中心控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<?> info(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setPassword(null);

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("userNo", user.getUserNo());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("gender", user.getGender());
        data.put("deptId", user.getDeptId());
        data.put("classId", user.getClassId());
        data.put("enrollmentYear", user.getEnrollmentYear());
        data.put("role", request.getAttribute("role"));

        // 查询班级名称
        if (user.getClassId() != null) {
            Clazz clazz = clazzMapper.selectById(user.getClassId());
            data.put("className", clazz != null ? clazz.getClassName() : "");
        } else {
            data.put("className", "");
        }

        // 查询院系名称
        if (user.getDeptId() != null) {
            Department dept = departmentMapper.selectById(user.getDeptId());
            data.put("deptName", dept != null ? dept.getDeptName() : "");
        } else {
            data.put("deptName", "");
        }

        return Result.success(data);
    }

    /**
     * 修改个人资料
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        User user = new User();
        user.setId(userId);
        if (params.containsKey("nickname"))
            user.setNickname((String) params.get("nickname"));
        if (params.containsKey("phone"))
            user.setPhone((String) params.get("phone"));
        if (params.containsKey("email"))
            user.setEmail((String) params.get("email"));
        if (params.containsKey("gender"))
            user.setGender((Integer) params.get("gender"));

        userMapper.updateById(user);
        return Result.success("修改成功", null);
    }

    /**
     * 修改密码（需校验旧密码，新密码 BCrypt 加密）
     */
    @PutMapping("/password")
    public Result<?> password(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("旧密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 校验旧密码（兼容明文和 BCrypt）
        boolean passwordMatch = oldPassword.equals(user.getPassword())
                || passwordEncoder.matches(oldPassword, user.getPassword());
        if (!passwordMatch) {
            return Result.error("旧密码错误");
        }

        // 加密新密码存入数据库
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", userId).set("password", encodedNewPassword);
        userMapper.update(null, wrapper);

        return Result.success("密码修改成功", null);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return (Long) userId;
        }
        // fallback
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                return JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {
                // ignore
            }
        }
        return 3L;
    }

    /**
     * 根据班级ID获取学生列表
     * 通过 sys_user_role 关联表查询角色为 student 的用户
     */
    /**
     * 根据班级ID获取学生列表
     * 通过 sys_user_role 关联表查询角色为 student 的用户
     */
    @GetMapping("/list-by-class")
    public Result<?> listByClass(@RequestParam Long classId) {
        // 构建查询条件，查询指定班级的学生（role_id = 3 表示学生角色）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", classId);
        wrapper.eq("is_deleted", 0);
        wrapper.inSql("id", "SELECT user_id FROM sys_user_role WHERE role_id = 3");
        wrapper.select("id", "nickname", "user_no");

        List<User> students = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = students.stream().map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("name", user.getNickname());
            item.put("userNo", user.getUserNo());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
