package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class LogEntry implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_no")
    private String userNo;

    @TableField("user_name")
    private String userName;

    @TableField("role_code")
    private String roleCode;

    @TableField("module")
    private String module;

    @TableField("action")
    private String action;

    @TableField("content")
    private String content;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_url")
    private String requestUrl;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_time")
    private LocalDateTime createTime;
}