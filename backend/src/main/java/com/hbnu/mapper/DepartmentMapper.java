package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院系 Mapper 接口
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}