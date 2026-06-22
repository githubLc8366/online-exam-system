package com.hbnu.service;

import com.hbnu.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 学生管理 Service 接口
 */
public interface StudentService {

  Result<?> batchResetPassword(List<Long> ids, String prefix, Boolean useSuffix);
  Result<?> resetPassword(Long id, String newPassword);

  /**
   * 分页查询学生列表（返回前端友好的字段名）
   */
  Result<Map<String, Object>> getList(Map<String, Object> params);

  /**
   * 新增学生
   */
  Result<?> add(Map<String, Object> data);

  /**
   * 编辑学生
   */
  Result<?> update(Map<String, Object> data);

  /**
   * 删除学生（逻辑删除）
   */
  Result<?> delete(Long id);

  /**
   * 批量导入学生
   */
  Result<?> batchImport(java.util.List<Map<String, Object>> students);

  /**
   * 修改学生账号状态
   */
  Result<?> updateStatus(Long id, Integer status);
}