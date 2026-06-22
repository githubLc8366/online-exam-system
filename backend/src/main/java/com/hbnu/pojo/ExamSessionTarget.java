package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试场次参与对象表 (exam_session_target)
 */
@Data
@TableName("exam_session_target")
public class ExamSessionTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考试场次ID */
    private Long sessionId;

    /** 目标类型：1=班级，2=学生（个体） */
    private Integer targetType;

    /** 目标ID：target_type=1时存储班级ID；target_type=2时存储用户ID */
    private Long targetId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic
    private Integer isDeleted;
}