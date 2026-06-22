package com.hbnu.service;

import com.hbnu.common.Result;

import java.util.Map;

/**
 * 考试记录 Service 接口
 */
public interface ExamRecordService {

  /**
   * 获取考试结果（查卷）
   */
  Result<?> getExamResult(Long recordId, Long userId);

  /**
   * 更新心跳时间（防作弊）
   */
  Result<?> updateHeartbeat(Long recordId, Long userId);

  /**
   * 上报切屏事件
   */
  Result<?> reportSwitch(Long recordId, Long userId);

  /**
   * 获取考试记录状态
   */
  Result<?> getRecordStatus(Long recordId, Long userId);

  /**
   * 保存答题进度（自动保存，不阅卷）
   */
  Result<?> saveProgress(Long recordId, Long userId, Map<String, Object> answers);
}