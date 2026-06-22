package com.hbnu.controller;

import com.hbnu.annotation.Log;
import com.hbnu.common.Result;
import com.hbnu.pojo.Department;
import com.hbnu.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 院系管理控制器
 */
@RestController
@RequestMapping("/api/admin")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取组织架构树（院系+班级）
     * GET /api/admin/organization/tree
     */
    @GetMapping("/organization/tree")
    public Result<List<Map<String, Object>>> getTree() {
        return departmentService.getTree();
    }

    /**
     * 获取院系树（仅院系，供 el-tree-select 使用）
     * GET /api/admin/department/tree
     */
    @GetMapping("/department/tree")
    public Result<List<Map<String, Object>>> tree() {
        return departmentService.tree();
    }

    /**
     * 获取院系列表
     * GET /api/admin/department/list
     */
    @GetMapping("/department/list")
    public Result<List<Department>> getDepartments() {
        return departmentService.getList();
    }

    /**
     * 新增院系
     * POST /api/admin/department
     */
    @Log(module = "组织架构", action = "新增院系")
    @PostMapping("/department")
    public Result<?> addDepartment(@RequestBody Department department) {
        return departmentService.add(department);
    }

    /**
     * 编辑院系
     * PUT /api/admin/department
     */
    @Log(module = "组织架构", action = "编辑院系")
    @PutMapping("/department")
    public Result<?> updateDepartment(@RequestBody Department department) {
        return departmentService.update(department);
    }

    /**
     * 删除院系
     * DELETE /api/admin/department/{id}
     */
    @Log(module = "组织架构", action = "删除院系")
    @DeleteMapping("/department/{id}")
    public Result<?> deleteDepartment(@PathVariable Long id) {
        return departmentService.delete(id);
    }
}