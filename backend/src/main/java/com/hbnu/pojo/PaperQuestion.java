package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷题目关联表 (exam_paper_question)
 */
@Data
@TableName("exam_paper_question")
public class PaperQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 题目ID */
    private Long questionId;

    /** 题目顺序 */
    private Integer questionOrder;

    /** 本题分值 */
    private BigDecimal score;

    /** 题目内容快照(含内容、选项、答案) */
//    @TableField(typeHandler = JacksonTypeHandler.class)
    private String questionSnapshot;

    /** 创建时间 */
    private LocalDateTime createTime;
}