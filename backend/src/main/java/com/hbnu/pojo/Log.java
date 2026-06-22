package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class Log implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userNo;
    private String userName;
    private String roleCode;
    private String module;
    private String action;
    private String content;
    private String requestMethod;
    private String requestUrl;
    private String ipAddress;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}