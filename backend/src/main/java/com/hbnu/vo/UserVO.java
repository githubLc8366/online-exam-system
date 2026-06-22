package com.hbnu.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String userNo;
    private String nickname;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private String deptName;   // 院系名称
    private String major;      // 专业
    private String className;   // 班级
    private List<String> roles;
    private String token;
}