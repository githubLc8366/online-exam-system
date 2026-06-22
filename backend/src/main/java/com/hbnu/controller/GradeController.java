package com.hbnu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.annotation.Log;
import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.mapper.ClazzMapper;
import com.hbnu.mapper.ExamRecordMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.Clazz;
import com.hbnu.pojo.ExamRecord;
import com.hbnu.pojo.User;
import com.hbnu.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * 成绩管理控制器（教师端）
 */
@RestController
@RequestMapping("/api/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClazzMapper clazzMapper;
    /**
     * 试题质量分析
     * GET /api/grade/question-quality/{sessionId}
     */
    @GetMapping("/question-quality/{sessionId}")
    public Result<?> questionQuality(@PathVariable Long sessionId) {
        return gradeService.questionQuality(sessionId);
    }

    /**
     * 主观题批阅
     */
    @Log(module = "成绩管理", action = "批阅试卷")
    @PostMapping("/mark")
    public Result<?> mark(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long recordId = Long.valueOf(params.get("recordId").toString());
        Long questionId = Long.valueOf(params.get("questionId").toString());
        BigDecimal score = new BigDecimal(params.get("score").toString());
        String comment = (String) params.get("comment");
        Long graderId = getCurrentUserId(request);
        return gradeService.markSubjective(recordId, questionId, score, comment, graderId);
    }

    /**
     * 考试成绩统计
     */
    @GetMapping("/statistics/{sessionId}")
    public Result<?> statistics(@PathVariable Long sessionId) {
        return gradeService.statistics(sessionId);
    }

    /**
     * 学生个人成绩趋势（各科平均分）
     * GET /api/grade/student-trend
     */
    @GetMapping("/student-trend")
    public Result<?> studentTrend(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return gradeService.studentTrend(userId);
    }

    /**
     * 学生知识点掌握度
     * GET /api/grade/knowledge-radar
     */
    @GetMapping("/knowledge-radar")
    public Result<?> knowledgeRadar(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return gradeService.knowledgeRadar(userId);
    }
    /**
     * 班级成绩对比数据
     * GET /api/grade/class-comparison/{sessionId}
     */
    @GetMapping("/class-comparison/{sessionId}")
    public Result<?> classComparison(@PathVariable Long sessionId) {
        return gradeService.classComparison(sessionId);
    }

    /**
     * 知识点掌握度数据（基于错题统计）
     * GET /api/grade/knowledge-mastery/{sessionId}
     */
    @GetMapping("/knowledge-mastery/{sessionId}")
    public Result<?> knowledgeMastery(@PathVariable Long sessionId) {
        return gradeService.knowledgeMastery(sessionId);
    }
    /**
     * 获取某场考试的所有成绩列表
     * GET /api/grade/score-list/{sessionId}
     */
    @GetMapping("/score-list/{sessionId}")
    public Result<?> scoreList(@PathVariable Long sessionId) {
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .eq("is_deleted", 0)
                .ge("status", 2)
                .orderByDesc("total_score");
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        // 批量查询学生班级信息
        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> classNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            List<Long> classIds = users.stream().map(User::getClassId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!classIds.isEmpty()) {
                List<Clazz> classes = clazzMapper.selectBatchIds(classIds);
                Map<Long, String> clazzNameMap = classes.stream().collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));
                for (User u : users) {
                    if (u.getClassId() != null && clazzNameMap.containsKey(u.getClassId())) {
                        classNameMap.put(u.getId(), clazzNameMap.get(u.getClassId()));
                    }
                }
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ExamRecord r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("recordId", r.getId());
            item.put("studentNo", r.getUserNo());
            item.put("name", r.getUserName());
            item.put("className", classNameMap.getOrDefault(r.getUserId(), "未知班级"));
            item.put("objectiveScore", r.getObjectiveScore());
            item.put("subjectiveScore", r.getSubjectiveScore());
            item.put("totalScore", r.getTotalScore());
            item.put("status", r.getStatus() == 4 ? "graded" : "pending");
            item.put("submitTime", r.getSubmitTime() != null ? r.getSubmitTime().toString().replace("T", " ") : "");
            item.put("abnormal", r.getCheatLogs() != null && !r.getCheatLogs().isEmpty());
            list.add(item);
        }

        return Result.success(list);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                return JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {
                // ignore
            }
        }
        return 2L; // 默认 teacher01
    }
}
