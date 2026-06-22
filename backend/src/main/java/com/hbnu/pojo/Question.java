package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题库表 (exam_question)
 */
@Data
@TableName(value = "exam_question", autoResultMap = true)
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 题型: 1=单选, 2=多选, 3=判断, 4=填空, 5=简答, 6=编程 */
    private Integer questionType;

    /** 题目内容 */
    private String content;

    /** 题目解析 */
    private String analysis;

    /** 正确答案(JSON格式) */
    private String answer;

    /** 选项列表(JSON) */
    private String options;

    /** 难度: 1=易, 2=较易, 3=中, 4=较难, 5=难 */
    private Integer difficulty;

    /** 科目 */
    private String subject;

    /** 知识点标签(JSON数组) */
    private String categoryTags;

    /** 默认分值 */
    private BigDecimal score;

    /** 创建教师ID */
    private Long creatorId;

    /** 使用次数 */
    private Integer usageCount;

    /** 正确率统计 */
    private BigDecimal correctRate;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    private String createBy;

    /** 更新人 */
    private String updateBy;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer isDeleted;
}