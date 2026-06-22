package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.Paper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷 Mapper 接口
 */
@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
}