package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 院系表 (sys_dept)
 */
@Data
@TableName("sys_dept")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父级院系ID */
    private Long parentId;

    /** 院系名称 */
    private String deptName;

    /** 备注 */
    private String remark;

    /** 排序号 */
    private Integer orderNum;

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