package com.hbnu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.Result;
import com.hbnu.mapper.*;
import com.hbnu.pojo.*;
import com.hbnu.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成绩管理 Service 实现类
 */
@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private WrongBookMapper wrongBookMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Override
    public Result<?> classComparison(Long sessionId) {
        // 查询该场次所有已完成/批阅完成的记录
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId).eq("is_deleted", 0).ge("status", 2);
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return Result.success(new HashMap<>());
        }

        // 获取所有考生ID
        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().collect(Collectors.toList());

        // 查询用户班级信息
        Map<Long, Long> userClassMap = new HashMap<>();
        Map<Long, String> classNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            List<Long> classIds = users.stream().map(User::getClassId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!classIds.isEmpty()) {
                List<Clazz> classes = clazzMapper.selectBatchIds(classIds);
                for (Clazz c : classes) {
                    classNameMap.put(c.getId(), c.getClassName());
                }
            }
            for (User u : users) {
                if (u.getClassId() != null) {
                    userClassMap.put(u.getId(), u.getClassId());
                }
            }
        }

        // 按班级分组统计
        Map<Long, List<BigDecimal>> classScores = new HashMap<>();
        for (ExamRecord r : records) {
            Long classId = userClassMap.getOrDefault(r.getUserId(), 0L);
            classScores.computeIfAbsent(classId, k -> new ArrayList<>()).add(r.getTotalScore());
        }

        // 组装返回数据
        List<String> classNames = new ArrayList<>();
        List<BigDecimal> avgScores = new ArrayList<>();
        List<BigDecimal> maxScores = new ArrayList<>();
        List<BigDecimal> minScores = new ArrayList<>();

        for (Map.Entry<Long, List<BigDecimal>> entry : classScores.entrySet()) {
            String className = classNameMap.getOrDefault(entry.getKey(), "未分班");
            classNames.add(className);

            List<BigDecimal> scores = entry.getValue();
            BigDecimal avg = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(scores.size()), 1, RoundingMode.HALF_UP);
            BigDecimal max = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            avgScores.add(avg);
            maxScores.add(max);
            minScores.add(min);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("classNames", classNames);
        data.put("avgScores", avgScores);
        data.put("maxScores", maxScores);
        data.put("minScores", minScores);

        return Result.success(data);
    }

    @Override
    public Result<?> knowledgeMastery(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error("考试场次不存在");
        }

        // 查询试卷中的所有题目
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", session.getPaperId());
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(pqWrapper);

        if (pqList.isEmpty()) {
            return Result.success(new HashMap<>());
        }

        // 查询该场次所有已提交的考试记录
        QueryWrapper<ExamRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("session_id", sessionId).eq("is_deleted", 0).ge("status", 2);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        int totalStudents = records.size();

        if (totalStudents == 0) {
            return Result.success(new HashMap<>());
        }

        // 统计每道题的正确人数
        Map<Long, Integer> correctCountMap = new HashMap<>();
        for (ExamRecord record : records) {
            String scoreDetailJson = record.getScoreDetail();
            if (scoreDetailJson == null || scoreDetailJson.isEmpty()) continue;

            try {
                Map<String, Object> scoreDetail = JSON.parseObject(scoreDetailJson);
                for (Map.Entry<String, Object> entry : scoreDetail.entrySet()) {
                    Long questionId = Long.valueOf(entry.getKey());
                    Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                    String status = (String) detail.get("status");
                    if ("correct".equals(status)) {
                        correctCountMap.merge(questionId, 1, Integer::sum);
                    } else if ("graded".equals(status)) {
                        Object scoreObj = detail.get("score");
                        double score = scoreObj != null ? Double.parseDouble(scoreObj.toString()) : 0;
                        if (score > 0) {
                            correctCountMap.merge(questionId, 1, Integer::sum);
                        }
                    }
                }
            } catch (Exception e) {
                // 跳过解析失败的
            }
        }

        // 组装图表数据
        List<String> labels = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();
        List<String> fullContents = new ArrayList<>();

        for (PaperQuestion pq : pqList) {
            String content = "";
            String snapshot = pq.getQuestionSnapshot();
            if (snapshot != null) {
                try {
                    Map<String, Object> snap = JSON.parseObject(snapshot);
                    content = (String) snap.getOrDefault("content", "");
                } catch (Exception e) {}
            }

            // 横轴只显示"题1"、"题2"...
            labels.add("题" + pq.getQuestionOrder());
            // 完整内容用于 tooltip
            fullContents.add(content);

            int correctCount = correctCountMap.getOrDefault(pq.getQuestionId(), 0);
            BigDecimal mastery = BigDecimal.valueOf(100.0 * correctCount / totalStudents)
                    .setScale(1, RoundingMode.HALF_UP);
            rates.add(mastery);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("knowledge", labels);
        data.put("mastery", rates);
        data.put("fullContents", fullContents);  // 新增

        return Result.success(data);
    }

    @Override
    @Transactional
    public Result<?> markSubjective(Long recordId, Long questionId, BigDecimal score, String comment, Long graderId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.error("考试记录不存在");
        }

        // 解析 score_detail
        String scoreDetailJson = record.getScoreDetail();
        Map<String, Map<String, Object>> scoreDetail;
        if (scoreDetailJson == null || scoreDetailJson.isEmpty()) {
            scoreDetail = new HashMap<>();
        } else {
            scoreDetail = JSON.parseObject(scoreDetailJson, new TypeReference<Map<String, Map<String, Object>>>() {});
        }

        // 更新该题的评分
        String qid = questionId.toString();
        Map<String, Object> item = scoreDetail.getOrDefault(qid, new HashMap<>());
        item.put("status", "graded");
        item.put("score", score);
        item.put("comment", comment);
        scoreDetail.put(qid, item);

        // 重新计算主观题总分：只累加已批阅(graded)的主观题得分，
        // 客观题(correct/wrong)的分值已计入 objectiveScore，避免重复累加导致总分翻倍
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        boolean allGraded = true;
        for (Map.Entry<String, Map<String, Object>> entry : scoreDetail.entrySet()) {
            String status = (String) entry.getValue().get("status");
            if ("pending".equals(status)) {
                allGraded = false;
            }
            if ("graded".equals(status)) {
                Object s = entry.getValue().get("score");
                if (s != null) {
                    subjectiveScore = subjectiveScore.add(new BigDecimal(s.toString()));
                }
            }
        }

        BigDecimal totalScore = record.getObjectiveScore().add(subjectiveScore);
        record.setSubjectiveScore(subjectiveScore);
        record.setTotalScore(totalScore);
        record.setScoreDetail(JSON.toJSONString(scoreDetail));
        record.setGraderId(graderId);
        record.setGradingTime(LocalDateTime.now());

        if (allGraded) {
            record.setStatus(4); // 全部批阅完成
        } else {
            record.setStatus(3); // 仍在批阅中
        }

        examRecordMapper.updateById(record);
        return Result.success("批阅成功", null);
    }

    @Override
    public Result<Map<String, Object>> statistics(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error("考试场次不存在");
        }

        // 【新增】获取试卷的及格线
        Paper paper = paperMapper.selectById(session.getPaperId());
        BigDecimal passLine = (paper != null && paper.getPassScore() != null)
                ? paper.getPassScore() : new BigDecimal("60");

        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .eq("is_deleted", 0)
                .ge("status", 2);
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            Map<String, Object> emptyMsg = new HashMap<>();
            emptyMsg.put("message", "暂无成绩数据");
            return Result.success("暂无成绩数据", emptyMsg);
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = records.get(0).getTotalScore() != null ?
                records.get(0).getTotalScore() : BigDecimal.ZERO;
        BigDecimal min = records.get(0).getTotalScore() != null ?
                records.get(0).getTotalScore() : BigDecimal.ZERO;
        int passCount = 0;
        int[] distribution = new int[5];

        for (ExamRecord r : records) {
            BigDecimal score = r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO;
            total = total.add(score);

            if (score.compareTo(max) > 0) max = score;
            if (score.compareTo(min) < 0) min = score;

            // 【修改】用试卷的及格线
            if (score.compareTo(passLine) >= 0) {
                passCount++;
            }

            int s = score.intValue();
            if (s < 60) distribution[0]++;
            else if (s < 70) distribution[1]++;
            else if (s < 80) distribution[2]++;
            else if (s < 90) distribution[3]++;
            else distribution[4]++;
        }

        BigDecimal avg = total.divide(new BigDecimal(records.size()), 2, RoundingMode.HALF_UP);
        BigDecimal passRate = new BigDecimal(passCount)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(records.size()), 2, RoundingMode.HALF_UP);

        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", records.size());
        data.put("maxScore", max);
        data.put("minScore", min);
        data.put("avgScore", avg);
        data.put("passRate", passRate);
        data.put("passLine", passLine);
        Map<String, Object> distributionMap = new HashMap<>();
        distributionMap.put("0-59", distribution[0]);
        distributionMap.put("60-69", distribution[1]);
        distributionMap.put("70-79", distribution[2]);
        distributionMap.put("80-89", distribution[3]);
        distributionMap.put("90-100", distribution[4]);
        data.put("distribution", distributionMap);

        return Result.success(data);
    }
    @Override
    public Result<?> studentTrend(Long userId) {
        // 查询该学生的所有已完成考试记录（status=4）
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 0)
                .eq("status", 4)
                .orderByAsc("create_time");
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        // 按科目分组，计算每个科目的平均分
        Map<String, List<BigDecimal>> subjectScores = new LinkedHashMap<>();
        for (ExamRecord record : records) {
            // 从试卷表中获取科目
            Paper paper = paperMapper.selectById(record.getPaperId());
            if (paper != null && paper.getSubject() != null) {
                String subject = paper.getSubject();
                subjectScores.computeIfAbsent(subject, k -> new ArrayList<>())
                        .add(record.getTotalScore());
            }
        }

        // 组装返回数据
        List<String> subjects = new ArrayList<>();
        List<BigDecimal> scores = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : subjectScores.entrySet()) {
            subjects.add(entry.getKey());
            BigDecimal avg = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP);
            scores.add(avg);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("subjects", subjects);
        data.put("scores", scores);
        return Result.success(data);
    }

    @Override
    public Result<?> knowledgeRadar(Long userId) {
        // 查询该学生的所有错题，按知识点分组统计正确率
        QueryWrapper<WrongBook> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 0);;
        List<WrongBook> wrongBooks = wrongBookMapper.selectList(wrapper);

        // 统计各知识点的错误次数
        Map<String, Integer> knowledgeWrongCount = new HashMap<>();
        for (WrongBook wb : wrongBooks) {
            // 从题目表中获取知识点
            Question question = questionMapper.selectById(wb.getQuestionId());
            if (question != null && question.getCategoryTags() != null) {
                String tags = question.getCategoryTags();
                try {
                    List<String> tagList = JSON.parseArray(tags, String.class);
                    for (String tag : tagList) {
                        knowledgeWrongCount.merge(tag, wb.getWrongCount(), Integer::sum);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // 简单估算掌握度（错误越多掌握度越低）
        List<String> knowledge = new ArrayList<>();
        List<Integer> mastery = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : knowledgeWrongCount.entrySet()) {
            knowledge.add(entry.getKey());
            // 掌握度 = 100 - 错误次数*10，最低0
            int score = Math.max(0, 100 - entry.getValue() * 10);
            mastery.add(score);
        }

        // 如果没有错题数据，返回默认
        if (knowledge.isEmpty()) {
            knowledge.addAll(Arrays.asList("面向对象", "算法", "网络协议", "数据库", "软件工程"));
            mastery.addAll(Arrays.asList(80, 60, 50, 85, 70));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("knowledge", knowledge);
        data.put("mastery", mastery);
        return Result.success(data);
    }
    @Override
    public Result<?> questionQuality(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error("考试场次不存在");
        }

        // 查询试卷所有题目
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", session.getPaperId()).orderByAsc("question_order");
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(pqWrapper);

        if (pqList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 查询所有已提交记录
        QueryWrapper<ExamRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("session_id", sessionId).eq("is_deleted", 0).ge("status", 2);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        int totalStudents = records.size();

        if (totalStudents == 0) {
            return Result.success(new ArrayList<>());
        }

        // 按总分排序，取前27%为高分组，后27%为低分组
        records.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));
        int groupSize = (int) Math.max(1, totalStudents * 0.27);

        List<ExamRecord> highGroup = records.subList(0, groupSize);
        List<ExamRecord> lowGroup = records.subList(totalStudents - groupSize, totalStudents);

        // 计算每道题的质量指标
        List<Map<String, Object>> result = new ArrayList<>();
        for (PaperQuestion pq : pqList) {
            Long qid = pq.getQuestionId();
            BigDecimal fullScore = pq.getScore();
            String typeLabel = typeName(pq);
            String content = getContent(pq);

            // 统计全体正确人数
            int totalCorrect = 0;
            int highCorrect = 0;
            int lowCorrect = 0;

            for (ExamRecord r : records) {
                int correct = isCorrectInRecord(r, qid);
                totalCorrect += correct;
            }
            for (ExamRecord r : highGroup) {
                highCorrect += isCorrectInRecord(r, qid);
            }
            for (ExamRecord r : lowGroup) {
                lowCorrect += isCorrectInRecord(r, qid);
            }

            // 难度系数 = 平均分 / 满分
            BigDecimal difficulty = totalStudents > 0
                    ? BigDecimal.valueOf((double) totalCorrect / totalStudents).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 区分度 = 高分组正确率 - 低分组正确率
            BigDecimal discrimination = BigDecimal.valueOf(
                    (double) highCorrect / groupSize - (double) lowCorrect / groupSize
            ).setScale(2, RoundingMode.HALF_UP);

            // 正确率
            BigDecimal correctRate = BigDecimal.valueOf((double) totalCorrect / totalStudents)
                    .setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> item = new HashMap<>();
            item.put("content", content.length() > 30 ? content.substring(0, 30) + "..." : content);
            item.put("type", typeLabel);
            item.put("score", fullScore);
            item.put("difficulty", difficulty);
            item.put("discrimination", discrimination);
            item.put("correctRate", correctRate);

            result.add(item);
        }

        return Result.success(result);
    }

    // 判断某道题在考试记录中是否正确
    private int isCorrectInRecord(ExamRecord record, Long questionId) {
        String detailJson = record.getScoreDetail();
        if (detailJson == null || detailJson.isEmpty()) return 0;

        try {
            Map<String, Object> detail = JSON.parseObject(detailJson);
            Map<String, Object> qDetail = (Map<String, Object>) detail.get(questionId.toString());
            if (qDetail == null) return 0;

            String status = (String) qDetail.get("status");
            if ("correct".equals(status)) return 1;
            if ("graded".equals(status)) {
                Object scoreObj = qDetail.get("score");
                return scoreObj != null && Double.parseDouble(scoreObj.toString()) > 0 ? 1 : 0;
            }
        } catch (Exception e) {}
        return 0;
    }

    // 获取题型中文名
    private String typeName(PaperQuestion pq) {
        String snapshot = pq.getQuestionSnapshot();
        if (snapshot != null) {
            try {
                Map<String, Object> snap = JSON.parseObject(snapshot);
                int type = ((Number) snap.get("questionType")).intValue();
                String[] names = {"", "单选", "多选", "判断", "填空", "简答", "编程"};
                return type < names.length ? names[type] : "未知";
            } catch (Exception e) {}
        }
        return "未知";
    }

    // 获取题目内容
    private String getContent(PaperQuestion pq) {
        String snapshot = pq.getQuestionSnapshot();
        if (snapshot != null) {
            try {
                Map<String, Object> snap = JSON.parseObject(snapshot);
                return (String) snap.getOrDefault("content", "");
            } catch (Exception e) {}
        }
        return "";
    }
}
