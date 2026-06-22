package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户表 (sys_user) - 学生/教师/管理员统一表
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学号/工号/账号 */
    private String userNo;

    /** 加密密码 */
    private String password;

    /** 姓名 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别: 1=男, 2=女 */
    private Integer gender;

    /** 院系ID */
    private Long deptId;

    /** 班级ID */
    private Long classId;

    /** 入学年份 */
    private String enrollmentYear;

    /** 职称（教师专用） */
    private String title;

    /** 入职日期（教师专用） */
    private String hireDate;

    /** 状态: 0=锁定, 1=正常 */
    private Integer status;

    /** 备注 */
    private String remark;

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