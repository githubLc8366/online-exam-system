package com.hbnu.config;

import com.alibaba.fastjson2.JSON;
import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${auth.whitelist:}")
    private List<String> whitelist;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // 检查是否在白名单中
        if (isWhitelisted(uri)) {
            return true;
        }

        // 获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            write401(response, "缺少 Authorization Token");
            return false;
        }

        token = token.replace("Bearer ", "");

        // 验证 Token
        if (!JwtUtils.validateToken(token)) {
            write401(response, "Token 无效或已过期");
            return false;
        }

        // 将用户信息存入 request 属性
        Long userId = JwtUtils.getUserId(token);
        String role = JwtUtils.getRole(token);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);

        // ========== 角色权限校验 ==========
        // 放行 /api/admin/class/list 等教师也需要访问的接口
        if (uri.startsWith("/api/admin/class/list") || uri.startsWith("/api/admin/department/list")) {
            if ("admin".equals(role) || "teacher".equals(role)) {
                return true;
            }
        }
        // 在权限校验中增加
        if (uri.startsWith("/api/teacher/")) {
            if ("admin".equals(role) || "teacher".equals(role)) {
                return true;
            }
            write403(response, "权限不足");
            return false;
        }
        // 【新增】放行教师修改学生状态的接口
        if (uri.startsWith("/api/admin/student/") && uri.endsWith("/status")) {
            if ("admin".equals(role) || "teacher".equals(role)) {
                return true;
            }
        }

        // 管理员接口：/api/admin/** 只能由 admin 角色访问
        if (uri.startsWith("/api/admin/") && !"admin".equals(role)) {
            write403(response, "权限不足：仅管理员可访问");
            return false;
        }

        // 题目管理接口：/api/question/**
        // GET 请求全部放行（学生错题本、组卷选题需要查看题目）
        // 写操作（POST/PUT/DELETE）只有教师和管理员可操作
        if (uri.startsWith("/api/question/")) {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            if (!"admin".equals(role) && !"teacher".equals(role)) {
                write403(response, "权限不足：仅教师和管理员可操作");
                return false;
            }
        }

        // 试卷管理接口：/api/paper/** 只能由教师和管理员访问
        if (uri.startsWith("/api/paper/") && !"admin".equals(role) && !"teacher".equals(role)) {
            write403(response, "权限不足：仅教师和管理员可访问");
            return false;
        }

        // 考试场次管理接口：/api/session/** 的写操作只能由教师和管理员访问
        if (uri.startsWith("/api/session/") && !"GET".equalsIgnoreCase(request.getMethod())) {
            if (!"admin".equals(role) && !"teacher".equals(role)) {
                write403(response, "权限不足：仅教师和管理员可操作");
                return false;
            }
        }

        // 成绩管理接口：/api/grade/**
        if (uri.startsWith("/api/grade/")) {
            if ("student".equals(role)) {
                // 学生只允许访问自己的成绩趋势和知识点雷达
                if (uri.contains("/student-trend") || uri.contains("/knowledge-radar")) {
                    return true;  // 放行
                }
                write403(response, "权限不足：学生无法访问此接口");
                return false;
            } else if (!"admin".equals(role) && !"teacher".equals(role)) {
                write403(response, "权限不足");
                return false;
            }
        }

        // 考试接口：/api/exam/**
        // result（查卷）接口允许教师访问（阅卷需要）
        // 其他接口只允许学生访问
        if (uri.startsWith("/api/exam/")) {
            // 查卷接口：教师和学生都可以访问
            if (uri.contains("/result/")) {
                if ("admin".equals(role) || "teacher".equals(role) || "student".equals(role)) {
                    return true;
                }
            }
            // 其他考试接口只有学生能访问
            if (!"student".equals(role)) {
                write403(response, "权限不足：仅学生可访问");
                return false;
            }
        }

        // 错题本接口：/api/wrong-book/** 学生可访问（其他角色也可查看自己的错题）
        // if (uri.startsWith("/api/wrong-book/")) {
        // return true;
        // }

        // 错题本接口：/api/wrong-book/** 只能由学生访问
        if (uri.startsWith("/api/wrong-book/")) {
            if (!"student".equals(role)) {
                write403(response, "权限不足：仅学生可访问");
                return false;
            }
            return true;
        }

        return true;
    }

    /**
     * 判断当前 URI 是否在白名单中
     */
    private boolean isWhitelisted(String uri) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        for (String pattern : whitelist) {
            // 处理 /** 通配符
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else if (uri.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private void write401(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.error(401, msg)));
    }

    private void write403(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.error(403, msg)));
    }
}