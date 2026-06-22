package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试场次表 (exam_session)
 */
@Data
@TableName("exam_session")
public class ExamSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考试名称 */
    private String sessionName;

    /** 关联试卷ID */
    private Long paperId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 发布状态: 0=未发布, 1=已发布 */
    private Integer publishStatus;

    /** 允许迟到: 0=不允许, 1=允许 */
    private Integer allowLate;

    /** 允许迟到分钟数: 0=不限制, >0=开考超过该分钟数禁止入场 */
    private Integer lateLimit;

    /** 开启防作弊 */
    private Integer cheatMonitor;

    /** 最大切屏次数 */
    private Integer maxSwitch;



    /** 创建教师ID */
    private Long creatorId;

    /** 参与人数 */
    private Integer participantCount;

    /** 完成人数 */
    private Integer completeCount;

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
    /** 快速交卷阈值(百分比)，低于总时长该比例标记为可疑 */
    private Integer fastSubmitThreshold;
    /** 题目乱序 0关闭 1开启 */
    private Integer shuffleQuestions;
    /** 选项乱序 0关闭 1开启 */
    private Integer shuffleOptions;
}