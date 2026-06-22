package com.hbnu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.Result;
import com.hbnu.mapper.ExamRecordMapper;
import com.hbnu.mapper.ExamSessionMapper;
import com.hbnu.mapper.PaperMapper;
import com.hbnu.mapper.QuestionMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.ExamRecord;
import com.hbnu.pojo.ExamSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard 统计数据控制器
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 教师端工作台统计数据
     */
    @GetMapping("/teacher/stats")
    public Result<Map<String, Object>> teacherStats() {
        Map<String, Object> stats = new HashMap<>();

        // 题库题目总数
        Long totalQuestions = questionMapper.selectCount(new QueryWrapper<>());
        stats.put("totalQuestions", totalQuestions);

        // 已组试卷总数
        Long totalPapers = paperMapper.selectCount(new QueryWrapper<>());
        stats.put("totalPapers", totalPapers);

        // 进行中考试数（当前时间在考试时间范围内的场次）
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<ExamSession> activeWrapper = new QueryWrapper<>();
        activeWrapper.le("start_time", now)
                .ge("end_time", now);
        Long activeExams = examSessionMapper.selectCount(activeWrapper);
        stats.put("activeExams", activeExams);

        // 待阅卷数（状态为 3=批阅中的考试记录）
        QueryWrapper<ExamRecord> gradingWrapper = new QueryWrapper<>();
        gradingWrapper.eq("status", 3);
        Long pendingGrading = examRecordMapper.selectCount(gradingWrapper);
        stats.put("pendingGrading", pendingGrading);

        return Result.success(stats);
    }

    /**
     * 管理端首页统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(new QueryWrapper<>()));
        stats.put("sessionCount", examSessionMapper.selectCount(new QueryWrapper<>()));
        stats.put("recordCount", examRecordMapper.selectCount(new QueryWrapper<>()));
        return Result.success(stats);
    }

    /**
     * 最近7天系统活跃趋势（每日新增考试记录数）
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        List<Map<String, Object>> list = examRecordMapper.selectDailyRecordTrend();
        return Result.success(list);
    }

    /**
     * 获取最近考试列表
     */
    @GetMapping("/teacher/recent-exams")
    public Result<List<Map<String, Object>>> recentExams() {
        QueryWrapper<ExamSession> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time").last("LIMIT 5");
        List<ExamSession> list = examSessionMapper.selectList(wrapper);

        List<Map<String, Object>> result = list.stream().map(session -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", session.getId());
            item.put("sessionName", session.getSessionName());
            item.put("startTime", session.getStartTime());
            item.put("endTime", session.getEndTime());
            item.put("paperId", session.getPaperId());

            // 计算时长（分钟）
            long duration = 0;
            if (session.getStartTime() != null && session.getEndTime() != null) {
                duration = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            }
            item.put("duration", duration);

            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
