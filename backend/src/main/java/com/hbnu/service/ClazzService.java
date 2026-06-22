package com.hbnu.service;

import com.hbnu.common.Result;
import com.hbnu.pojo.Clazz;

import java.util.List;
import java.util.Map;

/**
 * 班级 Service 接口
 */
public interface ClazzService {

    /**
     * 获取班级列表（支持分页参数）
     *
     * @param params 分页参数
     * @return Result 包含班级列表
     */
    Result<Map<String, Object>> getList(Map<String, Object> params);

    /**
     * 根据院系ID获取班级列表
     *
     * @param deptId 院系ID
     * @return Result 包含班级列表
     */
    Result<List<Clazz>> getByDeptId(Long deptId);

    /**
     * 新增班级
     *
     * @param clazz 班级对象
     * @return Result
     */
    Result<?> add(Clazz clazz);

    /**
     * 更新班级
     *
     * @param clazz 班级对象
     * @return Result
     */
    Result<?> update(Clazz clazz);

    /**
     * 删除班级
     *
     * @param id 班级ID
     * @return Result
     */
    Result<?> delete(Long id);
}