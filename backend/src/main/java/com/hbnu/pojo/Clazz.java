package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级表 (sys_class)
 * 使用Clazz避免与Java关键字class冲突
 */
@Data
@TableName("sys_class")
public class Clazz implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 班级名称 */
    private String className;

    /** 所属院系ID */
    private Long deptId;

    /** 入学年份 */
    private String enrollmentYear;

    /** 备注 */
    private String remark;

    /** 所属院系名称（非表字段，关联查询用） */
    @TableField(exist = false)
    private String deptName;

    /** 学生人数（非表字段，仅用于前端展示） */
    @TableField(exist = false)
    private Integer studentCount;

    /** 状态: 0=禁用, 1=启用 */
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