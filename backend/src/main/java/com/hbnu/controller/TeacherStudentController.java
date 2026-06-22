package com.hbnu.controller;

import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherStudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 教师查看自己所教班级的学生列表
     */
    @GetMapping("/student/list")
    public Result<Map<String, Object>> getList(@RequestParam Map<String, Object> params,
                                               HttpServletRequest request) {
        Long teacherId = getCurrentUserId(request);
        params.put("teacherId", teacherId);
        return studentService.getList(params);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                return JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {}
        }
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            return (Long) userIdObj;
        }
        return null;
    }
}