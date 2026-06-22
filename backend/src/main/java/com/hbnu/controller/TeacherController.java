package com.hbnu.controller;

import com.hbnu.annotation.Log;
import com.hbnu.common.Result;
import com.hbnu.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 教师管理控制器
 * 响应前端 /api/admin/teacher/* 请求
 */
@RestController
@RequestMapping("/api/admin")
public class TeacherController {

  @Autowired
  private TeacherService teacherService;

  /**
   * 获取教师列表（分页 + 搜索）
   * GET /api/admin/teacher/list
   */
  @GetMapping("/teacher/list")
  public Result<Map<String, Object>> getList(@RequestParam Map<String, Object> params) {
    return teacherService.getList(params);
  }

  /**
   * 新增教师
   * POST /api/admin/teacher
   */
  @Log(module = "教师管理", action = "新增教师")
  @PostMapping("/teacher")
  public Result<?> add(@RequestBody Map<String, Object> data) {
    return teacherService.add(data);
  }

  /**
   * 编辑教师
   * PUT /api/admin/teacher
   */
  @Log(module = "教师管理", action = "编辑教师")
  @PutMapping("/teacher")
  public Result<?> update(@RequestBody Map<String, Object> data) {
    return teacherService.update(data);
  }

  /**
   * 删除教师（逻辑删除）
   * DELETE /api/admin/teacher/{id}
   */
  @Log(module = "教师管理", action = "删除教师")
  @DeleteMapping("/teacher/{id}")
  public Result<?> delete(@PathVariable Long id) {
    return teacherService.delete(id);
  }

  /**
   * 修改教师账号状态
   * PUT /api/admin/teacher/{id}/status
   */
  @Log(module = "教师管理", action = "修改教师账号状态")
  @PutMapping("/teacher/{id}/status")
  public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : 1;
    return teacherService.updateStatus(id, status);
  }
}