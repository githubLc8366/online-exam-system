package com.hbnu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.Result;
import com.hbnu.mapper.ExamRecordMapper;
import com.hbnu.mapper.ExamSessionMapper;
import com.hbnu.pojo.ExamRecord;
import com.hbnu.pojo.ExamSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试记录控制器
 */
@RestController
@RequestMapping("/api/exam-record")
public class ExamRecordController {

  @Autowired
  private ExamRecordMapper examRecordMapper;

  @Autowired
  private ExamSessionMapper examSessionMapper;

  /**
   * 获取当前学生的考试列表
   */
  @GetMapping("/my-list")
  public Result<?> getMyExamList(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String keyword) {

    // TODO: 从登录上下文中获取当前学生ID（临时使用ID=1）
    Long currentStudentId = 1L;

    // 构建查询条件
    QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", currentStudentId);
    wrapper.eq("is_deleted", 0);

    if (keyword != null && !keyword.isEmpty()) {
      wrapper.like("paper_name", keyword);
    }

    wrapper.orderByDesc("create_time");

    // 分页查询
    Page<ExamRecord> pageParam = new Page<>(page, size);
    IPage<ExamRecord> recordPage = examRecordMapper.selectPage(pageParam, wrapper);

    // 构建返回结果
    List<Map<String, Object>> records = recordPage.getRecords().stream().map(record -> {
      Map<String, Object> item = new HashMap<>();
      item.put("id", record.getId());
      item.put("sessionId", record.getSessionId());
      item.put("paperName", record.getPaperName());
      item.put("userName", record.getUserName());
      item.put("status", record.getStatus());
      item.put("createTime", record.getCreateTime());

      // 关联查询考试场次信息
      ExamSession session = examSessionMapper.selectById(record.getSessionId());
      if (session != null) {
        item.put("sessionName", session.getSessionName());
        item.put("startTime", session.getStartTime());
        item.put("endTime", session.getEndTime());

        // 计算考试状态
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime endTime = session.getEndTime();

        String examStatus;
        if (now.isBefore(startTime)) {
          examStatus = "not_started"; // 未开始
        } else if (now.isAfter(endTime)) {
          examStatus = "ended"; // 已结束
        } else {
          examStatus = "ongoing"; // 进行中
        }
        item.put("examStatus", examStatus);
      }

      return item;
    }).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("records", records);
    result.put("total", recordPage.getTotal());
    result.put("pages", recordPage.getPages());
    result.put("current", recordPage.getCurrent());

    return Result.success(result);
  }
}