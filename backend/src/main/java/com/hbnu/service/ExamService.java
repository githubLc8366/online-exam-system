package com.hbnu.service;

import com.hbnu.common.Result;

import java.util.Map;

/**
 * 考试 Service 接口
 */
public interface ExamService {

    /**
     * 开始考试（获取题目，不含答案）
     *
     * @param sessionId 考试场次ID
     * @param userId    用户ID
     * @return Result
     */
    Result<?> startExam(Long sessionId, Long userId);

    /**
     * 提交答案并自动阅卷
     *
     * @param sessionId 考试场次ID
     * @param userId    用户ID
     * @param answers   学生答案 JSON
     * @return Result
     */
    Result<?> submitExam(Long sessionId, Long userId, Map<String, Object> answers);

    /**
     * 查询我的考试列表
     *
     * @param userId 用户ID
     * @return Result
     */
    Result<?> myExams(Long userId);
}