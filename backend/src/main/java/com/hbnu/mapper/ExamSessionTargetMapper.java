package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.ExamSessionTarget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试场次参与对象 Mapper 接口
 */
@Mapper
public interface ExamSessionTargetMapper extends BaseMapper<ExamSessionTarget> {
    // 不需要写任何代码，BaseMapper 已经提供了基本 CRUD 方法
}