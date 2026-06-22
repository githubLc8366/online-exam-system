package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.PaperQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷题目关联 Mapper 接口
 */
@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {

    /**
     * 根据试卷ID查询关联题目列表
     */
    List<PaperQuestion> selectByPaperId(@Param("paperId") Long paperId);
}