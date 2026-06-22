package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试记录表 (exam_record)
 */
@Data
@TableName("exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考试场次ID */
    private Long sessionId;

    /** 学生ID */
    private Long userId;

    /** 学号(冗余) */
    private String userNo;

    /** 学生姓名(冗余) */
    private String userName;

    /** 试卷ID(冗余) */
    private Long paperId;

    /** 试卷名称(冗余) */
    private String paperName;

    /** 实时答题内容(JSON) */
    private String currentAnswers;

    /** 最终答案(JSON) */
    private String finalAnswers;

    /** 得分明细(JSON) */
    private String scoreDetail;

    /** 客观题得分 */
    private BigDecimal objectiveScore;

    /** 主观题得分 */
    private BigDecimal subjectiveScore;

    /** 总分 */
    private BigDecimal totalScore;

    /** 防作弊记录(JSON) */
    private String cheatLogs;

    /** 切屏次数 */
    private Integer switchCount;

    /** IP地址 */
    private String ipAddress;

    /** 浏览器UA */
    private String userAgent;

    /** 开始考试时间 */
    private LocalDateTime startTime;

    /** 交卷时间 */
    private LocalDateTime submitTime;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeatTime;

    /** 交卷方式: 1=自主交卷, 2=超时自动交卷, 3=异常强制交卷 */
    private Integer submitType;

    /** 批阅时间 */
    private LocalDateTime gradingTime;

    /** 批阅教师ID */
    private Long graderId;

    /** 状态: 0=待考, 1=考试中, 2=已交卷, 3=批阅中, 4=已完成, 5=缺考 */
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
    /** 题目乱序序列(JSON) */
    private String questionOrder;
    /** 选项乱序映射(JSON) */
    private String optionOrder;
    /** 排名（非数据库字段，用于展示） */
    @TableField(exist = false)
    private Integer rank;
    /** 考试时长(分钟) - 非数据库字段，用于展示 */
    @TableField(exist = false)
    private Integer duration;
    /** 考试名称（非数据库字段，用于展示） */
    @TableField(exist = false)
    private String sessionName;

    /** 是否异常（非数据库字段，用于展示） */
    @TableField(exist = false)
    private Boolean abnormal;
}