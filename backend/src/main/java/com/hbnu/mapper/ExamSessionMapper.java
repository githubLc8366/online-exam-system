package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.ExamSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试场次 Mapper 接口
 */
@Mapper
public interface ExamSessionMapper extends BaseMapper<ExamSession> {
}