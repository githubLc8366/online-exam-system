package com.hbnu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.Result;
import com.hbnu.mapper.ClazzMapper;
import com.hbnu.mapper.DepartmentMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.Clazz;
import com.hbnu.pojo.Department;
import com.hbnu.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班级 Service 实现类
 */
@Service
public class ClazzServiceImpl implements ClazzService {

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<Map<String, Object>> getList(Map<String, Object> params) {
        QueryWrapper<Clazz> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);

        if (params.containsKey("deptId")) {
            queryWrapper.eq("dept_id", params.get("deptId"));
        }

        // 分页参数
        int page = 1, pageSize = 10;
        if (params.containsKey("page")) {
            page = Integer.parseInt(params.get("page").toString());
        }
        if (params.containsKey("pageSize")) {
            pageSize = Integer.parseInt(params.get("pageSize").toString());
        }

        queryWrapper.orderByAsc("id");

        // 先查总数
        long total = clazzMapper.selectCount(queryWrapper);

        // 分页查询
        queryWrapper.last("LIMIT " + (page - 1) * pageSize + ", " + pageSize);
        List<Clazz> clazzes = clazzMapper.selectList(queryWrapper);

        // 关联查询院系名称
        List<Department> allDepts = departmentMapper.selectList(
                new QueryWrapper<Department>().eq("is_deleted", 0));
        Map<Long, String> deptNameMap = allDepts.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        // 查询每个班级的学生人数，并填充院系名称
        for (Clazz clazz : clazzes) {
            // 填充院系名称
            clazz.setDeptName(deptNameMap.getOrDefault(clazz.getDeptId(), "未知"));

            // 填充学生人数
            QueryWrapper<com.hbnu.pojo.User> userWrapper = new QueryWrapper<>();
            userWrapper.eq("class_id", clazz.getId());
            userWrapper.eq("is_deleted", 0);
            clazz.setStudentCount(userMapper.selectCount(userWrapper).intValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", clazzes);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    @Override
    public Result<List<Clazz>> getByDeptId(Long deptId) {
        QueryWrapper<Clazz> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", deptId);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.orderByAsc("id");
        List<Clazz> clazzes = clazzMapper.selectList(queryWrapper);
        return Result.success(clazzes);
    }

    @Override
    public Result<?> add(Clazz clazz) {
        clazz.setCreateTime(LocalDateTime.now());
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.insert(clazz);
        return Result.success("添加成功");
    }

    @Override
    public Result<?> update(Clazz clazz) {
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.updateById(clazz);
        return Result.success("更新成功");
    }

    @Override
    public Result<?> delete(Long id) {
        // 检查该班级下是否有学生
        QueryWrapper<com.hbnu.pojo.User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("class_id", id);
        userQueryWrapper.eq("is_deleted", 0);
        long count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            return Result.error("该班级下存在 " + count + " 名学生，无法删除");
        }

        // 走 MP 的 deleteById 触发 @TableLogic 逻辑删除
        int affected = clazzMapper.deleteById(id);
        if (affected == 0) {
            return Result.error("删除失败，记录不存在或已被删除");
        }
        return Result.success("删除成功");
    }
}