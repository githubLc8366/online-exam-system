package com.hbnu.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题本表 (exam_wrong_book)
 */
@Data
@TableName("exam_wrong_book")
public class WrongBook implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 学生ID */
  private Long userId;

  /** 题目ID */
  private Long questionId;

  /** 所属科目(冗余方便查询) */
  private String subject;

  /** 出自考试名称(冗余方便查询) */
  private String examName;

  /** 做错次数 */
  private Integer wrongCount;

  /** 最近一次错选内容 */
  private String lastWrongAnswer;

  /** 是否已掌握: 0=否, 1=是 */
  private Integer isMastered;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 逻辑删除: 0=未删除, 1=已删除 */
  @TableLogic
  private Integer isDeleted;
}