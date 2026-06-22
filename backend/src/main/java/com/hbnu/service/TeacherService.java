package com.hbnu.service;

import com.hbnu.common.Result;

import java.util.Map;

/**
 * 教师管理 Service 接口
 */
public interface TeacherService {

  /**
   * 分页查询教师列表（返回前端友好的字段名）
   */
  Result<Map<String, Object>> getList(Map<String, Object> params);

  /**
   * 新增教师
   */
  Result<?> add(Map<String, Object> data);

  /**
   * 编辑教师
   */
  Result<?> update(Map<String, Object> data);

  /**
   * 删除教师（逻辑删除）
   */
  Result<?> delete(Long id);

  /**
   * 修改教师账号状态
   */
  Result<?> updateStatus(Long id, Integer status);
}