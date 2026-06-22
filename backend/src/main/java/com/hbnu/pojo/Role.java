package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色表 (sys_role)
 */
@Data
@TableName("sys_role")
public class Role implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /** 角色名称 */
    private String roleName;
    
    /** 角色编码: admin/teacher/student */
    private String roleCode;
    
    /** 角色描述 */
    private String description;
    
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