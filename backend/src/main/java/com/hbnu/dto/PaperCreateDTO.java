package com.hbnu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 创建试卷请求 DTO
 * 支持手动组卷和随机组卷两种模式
 */
@Data
public class PaperCreateDTO {

    // ==================== 基础信息 ====================
    // ==================== 基础信息 ====================

    /** 试卷ID（更新时必传） */
    private Long paperId;

    /** 试卷名称 */
    private String paperName;

    /** 总分（自动计算，前端可不传） */
    private BigDecimal totalScore;

    /** 及格线 */
    private BigDecimal passScore;

    /** 考试时长(分钟) */
    private Integer duration;

    /** 科目 */
    private String subject;

    /** 备注说明 */
    private String remark;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    // ==================== 组卷模式 ====================

    /**
     * 组卷模式
     * manual: 手动组卷（需传入 questions 列表）
     * random: 随机组卷（需传入 randomConfig 配置）
     */
    private String mode = "manual";

    // ==================== 手动组卷参数 ====================

    /** 题目列表（手动组卷时必填） */
    private List<PaperQuestionItemDTO> questions;

    // ==================== 随机组卷参数 ====================

    /** 随机组卷配置（随机组卷时必填） */
    private RandomConfigDTO randomConfig;

    // ==================== 内部类定义 ====================

    /**
     * 手动组卷 - 题目项
     */
    @Data
    public static class PaperQuestionItemDTO {
        /** 题目ID */
        private Long questionId;

        /** 题目顺序（在试卷中的排列位置） */
        private Integer questionOrder;

        /** 本题分值 */
        private BigDecimal score;
    }

    /**
     * 随机组卷 - 配置参数
     */
    @Data
    public static class RandomConfigDTO {

        // ---------- 各题型数量配置 ----------
        /** 单选题数量 */
        private Integer singleCount;

        /** 多选题数量 */
        private Integer multipleCount;

        /** 判断题数量 */
        private Integer judgeCount;

        /** 填空题数量 */
        private Integer fillCount;

        /** 简答题数量 */
        private Integer shortCount;

        /** 编程题数量 */
        private Integer programCount;

        // ---------- 各题型分值配置 ----------
        /** 单选题每题分值 */
        private BigDecimal singleScore;

        /** 多选题每题分值 */
        private BigDecimal multipleScore;

        /** 判断题每题分值 */
        private BigDecimal judgeScore;

        /** 填空题每题分值 */
        private BigDecimal fillScore;

        /** 简答题每题分值 */
        private BigDecimal shortScore;

        /** 编程题每题分值 */
        private BigDecimal programScore;

        // ---------- 难度比例配置 ----------
        /** 难度比例配置 */
        private DifficultyRatioDTO difficultyRatio;

        // ---------- 知识点比例配置 ----------
        /** 知识点比例配置：key=知识点名称, value=占比(%) */
        private Map<String, Integer> knowledgeRatio;
    }

    /**
     * 随机组卷 - 难度比例配置
     * 示例：easy=30, medium=50, hard=20 表示易30%、中50%、难20%
     */
    @Data
    public static class DifficultyRatioDTO {
        /** 容易题目占比（%） */
        private Integer easy;

        /** 中等题目占比（%） */
        private Integer medium;

        /** 困难题目占比（%） */
        private Integer hard;
    }
}