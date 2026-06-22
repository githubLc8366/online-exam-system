package com.hbnu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.Result;
import com.hbnu.mapper.ExamRecordMapper;
import com.hbnu.mapper.ExamSessionMapper;
import com.hbnu.mapper.PaperQuestionMapper;
import com.hbnu.pojo.ExamRecord;
import com.hbnu.pojo.ExamSession;
import com.hbnu.pojo.PaperQuestion;
import com.hbnu.service.ExamRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 考试记录 Service 实现类
 */
@Service
public class ExamRecordServiceImpl implements ExamRecordService {

  @Autowired
  private ExamSessionMapper examSessionMapper;

  @Autowired
  private ExamRecordMapper examRecordMapper;

  @Autowired
  private PaperQuestionMapper paperQuestionMapper;

  @Override
  public Result<?> getExamResult(Long recordId, Long userId) {
    // 1. 查询考试记录
    ExamRecord record = examRecordMapper.selectById(recordId);
    if (record == null || record.getIsDeleted() == 1) {  // 【新增】
      return Result.error("考试记录不存在");
    }


    // 3. 查询试卷题目关联（包含快照）
    QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
    pqWrapper.eq("paper_id", record.getPaperId()).orderByAsc("question_order");
    List<PaperQuestion> pqList = paperQuestionMapper.selectList(pqWrapper);

    // 4. 解析学生答案和得分详情
    Map<String, Object> finalAnswers = new HashMap<>();
    if (record.getFinalAnswers() != null && !record.getFinalAnswers().isEmpty()) {
      finalAnswers = JSON.parseObject(record.getFinalAnswers());
    }

    Map<String, Object> scoreDetail = new HashMap<>();
    if (record.getScoreDetail() != null && !record.getScoreDetail().isEmpty()) {
      scoreDetail = JSON.parseObject(record.getScoreDetail());
    }

    // 5. 组装答卷详情
    List<Map<String, Object>> questions = new ArrayList<>();
    for (PaperQuestion pq : pqList) {
      Map<String, Object> questionInfo = new HashMap<>();

      // 题目ID转为字符串
      String questionIdStr = String.valueOf(pq.getQuestionId());

      // 从快照中获取题目信息
      String questionSnapshot = pq.getQuestionSnapshot();
      if (questionSnapshot != null && !questionSnapshot.isEmpty()) {
        try {
          Map<String, Object> snapshot = JSON.parseObject(questionSnapshot);
          questionInfo.put("questionId", snapshot.get("id"));
          questionInfo.put("content", snapshot.get("content"));
          questionInfo.put("options", snapshot.get("options"));

          // 安全获取 questionType
          Object qTypeObj = snapshot.get("questionType");
          questionInfo.put("questionType", qTypeObj != null ? ((Number) qTypeObj).intValue() : null);

          // 格式化正确答案
          Object answerObj = snapshot.get("answer");
          String correctAnswerStr = "";
          if (answerObj != null) {
            if (answerObj instanceof List) {
              correctAnswerStr = String.join(", ", (List<String>) answerObj);
            } else {
              correctAnswerStr = answerObj.toString();
            }
          }
          questionInfo.put("answer", correctAnswerStr);

          questionInfo.put("analysis", snapshot.get("analysis"));
          questionInfo.put("score", pq.getScore());
        } catch (Exception e) {
          System.err.println("解析快照失败: " + e.getMessage());
          questionInfo.put("score", pq.getScore());
        }
      } else {
        questionInfo.put("score", pq.getScore());
      }

      // 获取学生答案
      Object studentAnswer = null;
      if (finalAnswers.containsKey(questionIdStr)) {
        studentAnswer = finalAnswers.get(questionIdStr);
      } else if (finalAnswers.containsKey(pq.getQuestionId())) {
        studentAnswer = finalAnswers.get(pq.getQuestionId());
      } else if (finalAnswers.containsKey(String.valueOf(pq.getQuestionOrder()))) {
        studentAnswer = finalAnswers.get(String.valueOf(pq.getQuestionOrder()));
      }

      // 格式化学生答案
      String studentAnswerStr = "";
      if (studentAnswer != null) {
        if (studentAnswer instanceof List) {
          studentAnswerStr = String.join(", ", (List<String>) studentAnswer);
        } else {
          studentAnswerStr = studentAnswer.toString();
        }
      }
      questionInfo.put("studentAnswer", studentAnswerStr.isEmpty() ? "（未作答）" : studentAnswerStr);

      // 获取得分详情
      if (scoreDetail.containsKey(questionIdStr)) {
        Map<String, Object> detail = (Map<String, Object>) scoreDetail.get(questionIdStr);
        questionInfo.put("status", detail.get("status"));
        Object gotScore = detail.get("score");
        questionInfo.put("gotScore", gotScore != null ? gotScore : 0);
      } else {
        questionInfo.put("status", "not_graded");
        questionInfo.put("gotScore", 0);
      }

      questions.add(questionInfo);
    }

    // 6. 组装返回数据
    Map<String, Object> result = new HashMap<>();
    result.put("recordId", record.getId());
    result.put("paperName", record.getPaperName());
    result.put("objectiveScore", record.getObjectiveScore());
    result.put("totalScore", record.getTotalScore());
    result.put("submitTime", record.getSubmitTime());
    result.put("questions", questions);
    // 【新增】计算排名
    int rank = 0;
    if (record.getSessionId() != null && record.getStatus() >= 2) {
      QueryWrapper<ExamRecord> rankWrapper = new QueryWrapper<>();
      rankWrapper.eq("session_id", record.getSessionId())
              .eq("is_deleted", 0)
              .ge("status", 2)
              .orderByDesc("total_score");
      List<ExamRecord> allRecords = examRecordMapper.selectList(rankWrapper);
      rank = 1;
      for (ExamRecord r : allRecords) {
        if (r.getId().equals(recordId)) break;
        rank++;
      }
    }
    result.put("rank", rank);

    // 【新增】计算用时
    String duration = "";
    if (record.getStartTime() != null && record.getSubmitTime() != null) {
      long seconds = java.time.Duration.between(record.getStartTime(), record.getSubmitTime()).getSeconds();
      if (seconds < 60) {
        duration = seconds + "秒";
      } else {
        duration = (seconds / 60) + "分钟";
      }
    }
    result.put("duration", duration);

    return Result.success(result);

  }

  @Override
  @Transactional
  public Result<?> updateHeartbeat(Long recordId, Long userId) {
    ExamRecord record = examRecordMapper.selectById(recordId);
    if (record == null || record.getIsDeleted() == 1) {
      return Result.error("考试记录不存在");
    }

    if (!record.getUserId().equals(userId)) {
      return Result.error("无权操作该考试记录");
    }

    // 更新心跳时间
    record.setLastHeartbeatTime(LocalDateTime.now());
    examRecordMapper.updateById(record);

    return Result.success("心跳更新成功");
  }

  @Override
  @Transactional
  public Result<?> reportSwitch(Long recordId, Long userId) {
    ExamRecord record = examRecordMapper.selectById(recordId);
    if (record == null || record.getIsDeleted() == 1) {
      return Result.error("考试记录不存在");
    }

    if (!record.getUserId().equals(userId)) {
      return Result.error("无权操作该考试记录");
    }

    // 切屏次数+1
    Integer currentSwitch = record.getSwitchCount() != null ? record.getSwitchCount() : 0;
    record.setSwitchCount(currentSwitch + 1);
    record.setLastHeartbeatTime(LocalDateTime.now());
    examRecordMapper.updateById(record);

    // 检查是否超过最大切屏次数
    ExamSession session = examSessionMapper.selectById(record.getSessionId());
    if (session != null && session.getCheatMonitor() == 1) {
      Integer maxSwitch = session.getMaxSwitch() != null ? session.getMaxSwitch() : 3;
      if (record.getSwitchCount() >= maxSwitch) {
        record.setStatus(2);
        record.setSubmitType(3);
        record.setSubmitTime(LocalDateTime.now());
        examRecordMapper.updateById(record);
        return Result.error("切屏次数超过限制，已自动交卷");
      }
    }

    return Result.success("切屏已记录");
  }
  @Override
  public Result<?> getRecordStatus(Long recordId, Long userId) {
    ExamRecord record = examRecordMapper.selectById(recordId);
    if (record == null || record.getIsDeleted() == 1) {
      return Result.error("考试记录不存在");
    }

    Map<String, Object> data = new HashMap<>();
    data.put("status", record.getStatus());
    data.put("submitType", record.getSubmitType());
    data.put("switchCount", record.getSwitchCount());

    return Result.success(data);
  }

  @Override
  @Transactional
  public Result<?> saveProgress(Long recordId, Long userId, Map<String, Object> answers) {
    ExamRecord record = examRecordMapper.selectById(recordId);
    if (record == null || record.getIsDeleted() == 1) {
      return Result.error("考试记录不存在");
    }
    if (!record.getUserId().equals(userId)) {
      return Result.error("无权操作该考试记录");
    }
    // 已交卷则不再保存，避免覆盖最终答案
    if (record.getStatus() != null && record.getStatus() >= 2) {
      return Result.error("考试已结束，无法保存");
    }
    if (answers != null) {
      record.setFinalAnswers(JSON.toJSONString(answers));
    }
    record.setLastHeartbeatTime(LocalDateTime.now());
    examRecordMapper.updateById(record);
    return Result.success("已保存");
  }
}