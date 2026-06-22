package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷表 (exam_paper)
 */
@Data
@TableName("exam_paper")
public class Paper implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String mode;
    
    /** 试卷名称 */
    private String paperName;
    
    /** 总分 */
    private BigDecimal totalScore;
    
    /** 及格线 */
    private BigDecimal passScore;
    
    /** 考试时长(分钟) */
    private Integer duration;
    
    /** 创建教师ID */
    private Long creatorId;
    
    /** 科目 */
    private String subject;
    
    /** 题目总数 */
    private Integer questionCount;
    
    /** 备注说明 */
    private String remark;
    
    /** 状态: 0=草稿, 1=发布, 2=归档 */
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