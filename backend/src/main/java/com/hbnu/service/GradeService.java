package com.hbnu.service;

import com.hbnu.common.Result;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 成绩管理 Service 接口
 */
public interface GradeService {

    /**
     * 主观题批阅
     *
     * @param recordId    考试记录ID
     * @param questionId  题目ID
     * @param score       评分
     * @param comment     评语
     * @param graderId    批阅教师ID
     * @return Result
     */
    Result<?> markSubjective(Long recordId, Long questionId, BigDecimal score, String comment, Long graderId);

    /**
     * 考试成绩统计
     *
     * @param sessionId 考试场次ID
     * @return Result
     */
    Result<Map<String, Object>> statistics(Long sessionId);

    /**
     * 学生个人成绩趋势（各科平均分）
     */
    Result<?> studentTrend(Long userId);

    /**
     * 学生知识点掌握度（雷达图数据）
     */
    Result<?> knowledgeRadar(Long userId);
    Result<?> classComparison(Long sessionId);
    Result<?> knowledgeMastery(Long sessionId);
    Result<?> questionQuality(Long sessionId);
}