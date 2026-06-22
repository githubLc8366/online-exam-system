package com.hbnu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.Result;
import com.hbnu.mapper.ClazzMapper;
import com.hbnu.mapper.DepartmentMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.Clazz;
import com.hbnu.pojo.Department;
import com.hbnu.pojo.User;
import com.hbnu.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 院系 Service 实现类
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<List<Map<String, Object>>> getTree() {
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.orderByAsc("order_num", "id");
        List<Department> departments = departmentMapper.selectList(queryWrapper);

        QueryWrapper<Clazz> clazzQueryWrapper = new QueryWrapper<>();
        clazzQueryWrapper.eq("status", 1);
        clazzQueryWrapper.eq("is_deleted", 0);
        List<Clazz> clazzes = clazzMapper.selectList(clazzQueryWrapper);

        // 查询每个班级的学生人数
        for (Clazz clazz : clazzes) {
            QueryWrapper<User> userWrapper = new QueryWrapper<>();
            userWrapper.eq("class_id", clazz.getId());
            userWrapper.eq("is_deleted", 0);
            userWrapper.eq("status", 1);
            clazz.setStudentCount(userMapper.selectCount(userWrapper).intValue());
        }

        Map<Long, List<Clazz>> clazzMap = clazzes.stream()
                .collect(Collectors.groupingBy(Clazz::getDeptId));

        List<Map<String, Object>> tree = buildTree(departments, clazzMap, null);
        return Result.success(tree);
    }

    @Override
    public Result<List<Map<String, Object>>> tree() {
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.orderByAsc("order_num", "id");
        List<Department> departments = departmentMapper.selectList(queryWrapper);

        List<Map<String, Object>> tree = buildDeptTree(departments, null);
        return Result.success(tree);
    }

    private List<Map<String, Object>> buildDeptTree(List<Department> departments, Long parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();

        List<Department> children = departments.stream()
                .filter(d -> (parentId == null && d.getParentId() == null)
                        || (parentId != null && parentId.equals(d.getParentId())))
                .collect(Collectors.toList());

        for (Department dept : children) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", dept.getId());
            node.put("label", dept.getDeptName());
            node.put("deptName", dept.getDeptName());
            node.put("orderNum", dept.getOrderNum());
            node.put("parentId", dept.getParentId());

            List<Map<String, Object>> childNodes = buildDeptTree(departments, dept.getId());
            if (!childNodes.isEmpty()) {
                node.put("children", childNodes);
            }

            nodes.add(node);
        }

        return nodes;
    }

    private List<Map<String, Object>> buildTree(List<Department> departments,
            Map<Long, List<Clazz>> clazzMap,
            Long parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();

        List<Department> children = departments.stream()
                .filter(d -> (parentId == null && d.getParentId() == null)
                        || (parentId != null && parentId.equals(d.getParentId())))
                .collect(Collectors.toList());

        for (Department dept : children) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", dept.getId());
            node.put("label", dept.getDeptName());
            node.put("deptName", dept.getDeptName());
            node.put("orderNum", dept.getOrderNum());
            node.put("parentId", dept.getParentId());

            List<Map<String, Object>> childNodes = buildTree(departments, clazzMap, dept.getId());

            List<Clazz> deptClasses = clazzMap.getOrDefault(dept.getId(), new ArrayList<>());
            for (Clazz clazz : deptClasses) {
                Map<String, Object> classNode = new HashMap<>();
                classNode.put("id", "class_" + clazz.getId());
                classNode.put("label", clazz.getClassName());
                classNode.put("className", clazz.getClassName());
                classNode.put("studentCount", clazz.getStudentCount());
                classNode.put("isClass", true);
                childNodes.add(classNode);
            }

            if (!childNodes.isEmpty()) {
                node.put("children", childNodes);
            }

            nodes.add(node);
        }

        return nodes;
    }

    @Override
    public Result<List<Department>> getList() {
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.orderByAsc("id");
        List<Department> departments = departmentMapper.selectList(queryWrapper);
        return Result.success(departments);
    }

    @Override
    public Result<?> add(Department department) {
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        departmentMapper.insert(department);
        return Result.success("添加成功");
    }

    @Override
    public Result<?> update(Department department) {
        department.setUpdateTime(LocalDateTime.now());
        departmentMapper.updateById(department);
        return Result.success("更新成功");
    }

    @Override
    public Result<?> delete(Long id) {
        // 检查是否存在班级
        QueryWrapper<Clazz> clazzQueryWrapper = new QueryWrapper<>();
        clazzQueryWrapper.eq("dept_id", id);
        clazzQueryWrapper.eq("is_deleted", 0);
        if (clazzMapper.selectCount(clazzQueryWrapper) > 0) {
            return Result.error("该院系下存在班级，无法删除");
        }

        // 检查是否存在下级院系
        QueryWrapper<Department> deptQueryWrapper = new QueryWrapper<>();
        deptQueryWrapper.eq("parent_id", id);
        deptQueryWrapper.eq("is_deleted", 0);
        if (departmentMapper.selectCount(deptQueryWrapper) > 0) {
            return Result.error("该院系下存在子级院系，无法删除");
        }

        // 使用 MP 的 deleteById 触发逻辑删除（@TableLogic 字段在 updateById 中会被剔除，必须走 deleteById）
        int affected = departmentMapper.deleteById(id);
        if (affected == 0) {
            return Result.error("删除失败，记录不存在或已被删除");
        }
        return Result.success("删除成功");
    }
}