package com.hbnu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hbnu.common.Result;
import com.hbnu.mapper.QuestionMapper;
import com.hbnu.pojo.Question;
import com.hbnu.service.QuestionService;
import org.springframework.stereotype.Service;

/**
 * 题目 Service 实现类
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    public Result<IPage<Question>> pageList(Integer page, Integer size, Integer questionType, String subject,
                                            Integer difficulty) {
        Page<Question> pageParam = new Page<>(page, size);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();

        // 【修复】过滤已删除的记录
        queryWrapper.eq("is_deleted", 0);

        if (questionType != null) {
            queryWrapper.eq("question_type", questionType);
        }
        if (subject != null && !subject.isEmpty()) {
            queryWrapper.eq("subject", subject);
        }
        if (difficulty != null) {
            queryWrapper.eq("difficulty", difficulty);
        }

        queryWrapper.orderByDesc("create_time");

        IPage<Question> result = this.page(pageParam, queryWrapper);
        return Result.success(result);
    }

    @Override
    public Result<?> add(Question question, Long creatorId) {
        if (creatorId == null) {
            return Result.error("无法获取当前登录用户信息");
        }
        question.setCreatorId(creatorId);
        if (question.getScore() == null) question.setScore(new java.math.BigDecimal("2.0"));
        if (question.getDifficulty() == null) question.setDifficulty(3);
        if (question.getStatus() == null) question.setStatus(1);
        if (question.getUsageCount() == null) question.setUsageCount(0);

        boolean saved = this.save(question);
        return saved ? Result.success("新增成功") : Result.error("新增失败");
    }

    @Override
    public Result<?> delete(Long id) {
        boolean removed = this.removeById(id);
        if (removed) {
            return Result.success("删除成功", null);
        } else {
            return Result.error("删除失败");
        }
    }
}
