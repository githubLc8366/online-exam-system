package com.hbnu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.Result;
import com.hbnu.mapper.QuestionMapper;
import com.hbnu.mapper.WrongBookMapper;
import com.hbnu.pojo.Question;
import com.hbnu.pojo.WrongBook;
import com.hbnu.service.WrongBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 错题本 Service 实现类
 */
@Service
public class WrongBookServiceImpl implements WrongBookService {

  @Autowired
  private WrongBookMapper wrongBookMapper;
  @Autowired
  private QuestionMapper questionMapper;

  @Override
  public Result<?> pageList(Integer page, Integer size, Long userId, String subject, Integer isMastered) {
    Page<WrongBook> pageParam = new Page<>(page, size);
    QueryWrapper<WrongBook> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("user_id", userId);
    if (subject != null && !subject.isEmpty()) {
      queryWrapper.eq("subject", subject);
    }
    if (isMastered != null) {
      queryWrapper.eq("is_mastered", isMastered);
    }
    queryWrapper.orderByDesc("create_time");

    IPage<WrongBook> result = wrongBookMapper.selectPage(pageParam, queryWrapper);

    // 为每条错题关联查询题目内容
    List<Map<String, Object>> enrichedList = new ArrayList<>();
    for (WrongBook wb : result.getRecords()) {
      Map<String, Object> item = new HashMap<>();
      item.put("id", wb.getId());
      item.put("userId", wb.getUserId());
      item.put("questionId", wb.getQuestionId());
      item.put("subject", wb.getSubject());
      item.put("wrongCount", wb.getWrongCount());
      item.put("isMastered", wb.getIsMastered());
      item.put("lastWrongAnswer", wb.getLastWrongAnswer());
      item.put("createTime", wb.getCreateTime());
      item.put("updateTime", wb.getUpdateTime());
      item.put("examName", wb.getExamName() != null ? wb.getExamName() : "");

      // 查询题目内容
      Question question = questionMapper.selectById(wb.getQuestionId());
      if (question != null) {
        item.put("questionType", question.getQuestionType());
        item.put("content", question.getContent());
        item.put("correctAnswer", question.getAnswer());
        item.put("analysis", question.getAnalysis());
        // 选项
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
          try {
            item.put("options", JSON.parseArray(question.getOptions()));
          } catch (Exception e) {
            item.put("options", new ArrayList<>());
          }
        } else {
          item.put("options", new ArrayList<>());
        }
      }

      enrichedList.add(item);
    }

    Map<String, Object> data = new HashMap<>();
    data.put("records", enrichedList);
    data.put("total", result.getTotal());
    data.put("page", result.getCurrent());
    data.put("pageSize", result.getSize());

    return Result.success("success", data);
  }

  @Override
  @Transactional
  public Result<?> addWrongBook(WrongBook wrongBook) {
    // 检查是否已存在相同的错题记录
    QueryWrapper<WrongBook> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("user_id", wrongBook.getUserId());
    queryWrapper.eq("question_id", wrongBook.getQuestionId());
    queryWrapper.eq("is_deleted", 0);      // ← 加上这行

    WrongBook existing = wrongBookMapper.selectOne(queryWrapper);
    if (existing != null) {
      // 如果已存在，更新错题次数和最近错选答案
      existing.setWrongCount(existing.getWrongCount() != null ? existing.getWrongCount() + 1 : 1);
      existing.setLastWrongAnswer(wrongBook.getLastWrongAnswer());
      existing.setUpdateTime(LocalDateTime.now());
      wrongBookMapper.updateById(existing);
    } else {
      // 如果不存在，新增记录
      wrongBook.setWrongCount(wrongBook.getWrongCount() != null ? wrongBook.getWrongCount() : 1);
      wrongBook.setIsDeleted(0);           // ← 确保 is_deleted 默认值
      wrongBook.setCreateTime(LocalDateTime.now());
      wrongBook.setUpdateTime(LocalDateTime.now());
      wrongBookMapper.insert(wrongBook);
    }

    return Result.success("添加成功");
  }

  @Override
  @Transactional
  public Result<?> updateMasteredStatus(Long id, Integer isMastered) {
    WrongBook wrongBook = wrongBookMapper.selectById(id);
    if (wrongBook == null) {
      return Result.error("错题记录不存在");
    }

    wrongBook.setIsMastered(isMastered);
    wrongBook.setUpdateTime(LocalDateTime.now());
    wrongBookMapper.updateById(wrongBook);

    return Result.success("更新成功");
  }

  @Override
  @Transactional
  public Result<?> deleteWrongBook(Long id) {
    WrongBook wrongBook = wrongBookMapper.selectById(id);
    if (wrongBook == null) {
      return Result.error("错题记录不存在");
    }

    wrongBookMapper.deleteById(id);
    return Result.success("删除成功");
  }

  @Override
  @Transactional
  public Result<?> batchDeleteWrongBook(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Result.error("请选择要删除的错题记录");
    }

    wrongBookMapper.deleteBatchIds(ids);
    return Result.success("批量删除成功");
  }
}