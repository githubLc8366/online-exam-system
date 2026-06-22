package com.hbnu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hbnu.common.Result;
import com.hbnu.pojo.Question;

/**
 * 题目 Service 接口
 */
public interface QuestionService extends IService<Question> {

    /**
     * 分页查询题目列表
     *
     * @param page         当前页
     * @param size         每页大小
     * @param questionType 题型
     * @param subject      科目
     * @param difficulty   难度
     * @return Result
     */
    Result<IPage<Question>> pageList(Integer page, Integer size, Integer questionType, String subject,
            Integer difficulty);

    /**
     * 新增题目
     *
     * @param question  题目信息
     * @param creatorId 创建人ID
     * @return Result
     */
    Result<?> add(Question question, Long creatorId);

    /**
     * 逻辑删除题目
     *
     * @param id 题目ID
     * @return Result
     */
    Result<?> delete(Long id);
}