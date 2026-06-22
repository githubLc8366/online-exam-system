package com.hbnu.controller;

import com.hbnu.annotation.Log;
import com.hbnu.common.Result;
import com.hbnu.pojo.Clazz;
import com.hbnu.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 班级管理控制器
 */
@RestController
@RequestMapping("/api/admin")
public class ClazzController {

    @Autowired
    private ClazzService clazzService;

    /**
     * 获取班级列表（支持分页参数）
     * GET /api/admin/class/list
     */
    @GetMapping("/class/list")
    public Result<Map<String, Object>> getClasses(@RequestParam Map<String, Object> params) {
        return clazzService.getList(params);
    }

    /**
     * 新增班级
     * POST /api/admin/class
     */
    @Log(module = "组织架构", action = "新增班级")
    @PostMapping("/class")
    public Result<?> addClass(@RequestBody Clazz clazz) {
        return clazzService.add(clazz);
    }

    /**
     * 编辑班级
     * PUT /api/admin/class
     */
    @Log(module = "组织架构", action = "编辑班级")
    @PutMapping("/class")
    public Result<?> updateClass(@RequestBody Clazz clazz) {
        return clazzService.update(clazz);
    }

    /**
     * 删除班级
     * DELETE /api/admin/class/{id}
     */
    @Log(module = "组织架构", action = "删除班级")
    @DeleteMapping("/class/{id}")
    public Result<?> deleteClass(@PathVariable Long id) {
        return clazzService.delete(id);
    }
}