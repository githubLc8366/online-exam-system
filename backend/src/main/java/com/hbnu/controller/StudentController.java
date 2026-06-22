package com.hbnu.controller;

import com.hbnu.annotation.Log;
import com.hbnu.common.Result;
import com.hbnu.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生管理控制器
 * 响应前端 /api/admin/student/* 请求
 */
@RestController
@RequestMapping("/api/admin")
public class StudentController {

  @Autowired
  private StudentService studentService;

  /**
   * 获取学生列表（分页 + 搜索）
   * GET /api/admin/student/list
   */
  @GetMapping("/student/list")
  public Result<Map<String, Object>> getList(@RequestParam Map<String, Object> params) {
    return studentService.getList(params);
  }

  /**
   * 新增学生
   * POST /api/admin/student
   */
  @Log(module = "学生管理", action = "新增学生")
  @PostMapping("/student")
  public Result<?> add(@RequestBody Map<String, Object> data) {
    return studentService.add(data);
  }

  /**
   * 编辑学生
   * PUT /api/admin/student
   */
  @Log(module = "学生管理", action = "编辑学生")
  @PutMapping("/student")
  public Result<?> update(@RequestBody Map<String, Object> data) {
    return studentService.update(data);
  }

  /**
   * 删除学生（逻辑删除）
   * DELETE /api/admin/student/{id}
   */
  @Log(module = "学生管理", action = "删除学生")
  @DeleteMapping("/student/{id}")
  public Result<?> delete(@PathVariable Long id) {
    return studentService.delete(id);
  }

  /**
   * 批量导入学生
   * POST /api/admin/student/import
   */
  @Log(module = "学生管理", action = "批量导入学生")
  @PostMapping("/student/import")
  public Result<?> batchImport(@RequestBody List<Map<String, Object>> students) {
    return studentService.batchImport(students);
  }

  /**
   * 修改学生账号状态
   * PUT /api/admin/student/{id}/status
   */
  @PutMapping("/student/{id}/status")
  public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : 1;
    return studentService.updateStatus(id, status);
  }
  /**
   * 重置学生密码
   * PUT /api/admin/student/{id}/reset-password
   */
  @Log(module = "学生管理", action = "重置密码")
  @PutMapping("/student/{id}/reset-password")
  public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    String newPassword = body.get("password") != null ? body.get("password").toString() : null;
    if (newPassword == null || newPassword.trim().isEmpty()) {
      return Result.error("密码不能为空");
    }
    if (newPassword.length() < 6) {
      return Result.error("密码长度不能少于6位");
    }
    return studentService.resetPassword(id, newPassword);
  }

  /**
   * 批量重置学生密码（按规则生成）
   * POST /api/admin/student/batch-reset-password
   */
  @Log(module = "学生管理", action = "批量重置密码")
  @PostMapping("/student/batch-reset-password")
  public Result<?> batchResetPassword(@RequestBody Map<String, Object> body) {
    List<Object> rawIds = (List<Object>) body.get("ids");
    List<Long> ids = rawIds.stream()
            .map(id -> Long.valueOf(id.toString()))
            .collect(Collectors.toList());

    String prefix = body.get("prefix") != null ? body.get("prefix").toString() : "";
    Boolean useSuffix = body.get("useSuffix") != null && (Boolean) body.get("useSuffix");

    if (ids.isEmpty()) {
      return Result.error("请选择学生");
    }

    return studentService.batchResetPassword(ids, prefix, useSuffix);
  }
}