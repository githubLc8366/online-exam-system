package com.hbnu.controller;

import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.service.ExamService;
import com.hbnu.service.ExamRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamRecordService examRecordService;

    /**
     * 上报切屏事件
     */
    @PostMapping("/switch-report/{recordId}")
    public Result<?> reportSwitch(@PathVariable Long recordId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return examRecordService.reportSwitch(recordId, userId);
    }

    /**
     * 获取考试记录状态
     */
    @GetMapping("/record-status/{recordId}")
    public Result<?> getRecordStatus(@PathVariable Long recordId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return examRecordService.getRecordStatus(recordId, userId);
    }

    /**
     * 开始考试 - 获取题目（不含答案）
     */
    @GetMapping("/start/{sessionId}")
    public Result<?> start(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return examService.startExam(sessionId, userId);
    }

    /**
     * 提交答案 - 自动阅卷
     */
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) params.get("answers");
        Long userId = getCurrentUserId(request);
        return examService.submitExam(sessionId, userId, answers);
    }

    /**
     * 我的考试列表
     */
    @GetMapping("/my-exams")
    public Result<?> myExams(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return examService.myExams(userId);
    }

    /**
     * 获取考试结果（查卷）
     */
    @GetMapping("/result/{recordId}")
    public Result<?> getExamResult(@PathVariable Long recordId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return examRecordService.getExamResult(recordId, userId);
    }

    /**
     * 考试心跳接口（防作弊）
     */
    @PostMapping("/heartbeat/{recordId}")
    public Result<?> heartbeat(@PathVariable Long recordId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return examRecordService.updateHeartbeat(recordId, userId);
    }

    /**
     * 保存答题进度（自动保存，不阅卷）
     */
    @PostMapping("/save-progress/{recordId}")
    public Result<?> saveProgress(@PathVariable Long recordId, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = params.get("answers") instanceof Map ? (Map<String, Object>) params.get("answers") : null;
        return examRecordService.saveProgress(recordId, userId, answers);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                Long userId = JwtUtils.getUserId(token.replace("Bearer ", ""));
                if (userId != null) {
                    return userId;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            return (Long) userIdObj;
        }
        return null;
    }
}