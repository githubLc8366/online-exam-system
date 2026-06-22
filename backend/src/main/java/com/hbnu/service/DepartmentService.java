package com.hbnu.service;

import com.hbnu.common.Result;
import com.hbnu.pojo.Department;

import java.util.List;
import java.util.Map;

/**
 * 院系 Service 接口
 */
public interface DepartmentService {

    /**
     * 获取组织架构树（院系+班级）
     *
     * @return Result 包含树形结构数据
     */
    Result<List<Map<String, Object>>> getTree();

    /**
     * 获取院系树（仅院系，递归结构）
     *
     * @return Result 包含院系树形数据
     */
    Result<List<Map<String, Object>>> tree();

    /**
     * 获取院系列表
     *
     * @return Result 包含院系列表
     */
    Result<List<Department>> getList();

    /**
     * 新增院系
     *
     * @param department 院系对象
     * @return Result
     */
    Result<?> add(Department department);

    /**
     * 更新院系
     *
     * @param department 院系对象
     * @return Result
     */
    Result<?> update(Department department);

    /**
     * 删除院系
     *
     * @param id 院系ID
     * @return Result
     */
    Result<?> delete(Long id);
}
