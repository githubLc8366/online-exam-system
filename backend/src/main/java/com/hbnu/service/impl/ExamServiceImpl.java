package com.hbnu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hbnu.common.Result;
import com.hbnu.mapper.*;
import com.hbnu.pojo.*;
import com.hbnu.service.ExamService;
import com.hbnu.service.WrongBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 考试 Service 实现类
 */
@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private WrongBookService wrongBookService;
    // 【新增】获取真实IP的方法
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    @Override
    public Result<?> startExam(Long sessionId, Long userId) {
        // 1. 校验考试记录
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .eq("user_id", userId)
                .eq("is_deleted", 0);;
        ExamRecord record = examRecordMapper.selectOne(wrapper);
        if (record == null) {
            return Result.error("未找到考试记录");
        }
        if (record.getStatus() > 1) {
            return Result.error("考试已结束");
        }

        // 1.1 校验考试时间窗口：未到开始时间 / 已过结束时间均不允许进入
        ExamSession sessionForTime = examSessionMapper.selectById(sessionId);
        if (sessionForTime != null) {
            LocalDateTime nowTime = LocalDateTime.now();
            if (sessionForTime.getStartTime() != null && nowTime.isBefore(sessionForTime.getStartTime())) {
                return Result.error("考试尚未开始，请在开始时间后进入");
            }
            if (sessionForTime.getEndTime() != null && nowTime.isAfter(sessionForTime.getEndTime())) {
                return Result.error("考试已结束，无法进入");
            }
            // 迟到限制：开考超过 lateLimit 分钟后，待考(未入场)的考生禁止进入
            Integer lateLimit = sessionForTime.getLateLimit();
            if (record.getStatus() == 0 && lateLimit != null && lateLimit > 0
                    && sessionForTime.getStartTime() != null
                    && nowTime.isAfter(sessionForTime.getStartTime().plusMinutes(lateLimit))) {
                return Result.error("已超过允许迟到时间（" + lateLimit + " 分钟），无法进入考试");
            }
        }

        // 2. 更新状态为考试中，记录IP和UA
        record.setStatus(1);
        record.setStartTime(LocalDateTime.now());

        // 【新增】获取并记录IP地址和User-Agent
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = getClientIp(request);
                record.setIpAddress(ip);

                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 500) {
                    userAgent = userAgent.substring(0, 500);
                }
                record.setUserAgent(userAgent);
            }
        } catch (Exception e) {
            // 忽略
        }
        examRecordMapper.updateById(record);

        // 3. 查询试卷题目（不含答案）
        Long paperId = record.getPaperId();
        Paper paper = paperMapper.selectById(paperId);
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", paperId).orderByAsc("question_order");
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(pqWrapper);

        List<Long> questionIds = new ArrayList<>();
        for (PaperQuestion pq : pqList) {
            questionIds.add(pq.getQuestionId());
        }

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            q.setAnswer(null); // 屏蔽答案
            questionMap.put(q.getId(), q);
        }

        // 4. 组装返回数据（带顺序和分值）
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (PaperQuestion pq : pqList) {
            Map<String, Object> item = new HashMap<>();
            Question q = questionMap.get(pq.getQuestionId());
            if (q != null) {
                item.put("questionId", q.getId());
                item.put("questionType", q.getQuestionType());
                item.put("content", q.getContent());
                item.put("options", q.getOptions());
                item.put("difficulty", q.getDifficulty());
                item.put("subject", q.getSubject());
                item.put("score", pq.getScore());
                item.put("questionOrder", pq.getQuestionOrder());
                resultList.add(item);
            }
        }

        // 查询场次配置
        ExamSession session = examSessionMapper.selectById(sessionId);
        Integer threshold = (session != null && session.getFastSubmitThreshold() != null)
                ? session.getFastSubmitThreshold() : 20;

        // 5. 题目乱序 & 选项乱序
        boolean needSave = false;
        if (session != null && session.getShuffleQuestions() != null && session.getShuffleQuestions() == 1) {
            if (record.getQuestionOrder() == null || record.getQuestionOrder().isEmpty()) {
                // 首次进入：打乱题目顺序
                Collections.shuffle(resultList);
                com.alibaba.fastjson2.JSONArray orderArr = new com.alibaba.fastjson2.JSONArray();
                for (Map<String, Object> item : resultList) {
                    orderArr.add(item.get("questionId"));
                }
                record.setQuestionOrder(orderArr.toJSONString());
                needSave = true;
            } else {
                // 刷新页面：按存储的顺序重排
                com.alibaba.fastjson2.JSONArray orderArr = com.alibaba.fastjson2.JSON.parseArray(record.getQuestionOrder());
                Map<Object, Map<String, Object>> qMap = new HashMap<>();
                for (Map<String, Object> item : resultList) {
                    qMap.put(item.get("questionId"), item);
                }
                List<Map<String, Object>> ordered = new ArrayList<>();
                for (int i = 0; i < orderArr.size(); i++) {
                    Map<String, Object> item = qMap.get(orderArr.getLong(i));
                    if (item != null) ordered.add(item);
                }
                resultList.clear();
                resultList.addAll(ordered);
            }
        }

        if (session != null && session.getShuffleOptions() != null && session.getShuffleOptions() == 1) {
            com.alibaba.fastjson2.JSONObject optOrderMap;
            boolean isFirstTime = record.getOptionOrder() == null || record.getOptionOrder().isEmpty();
            if (isFirstTime) {
                optOrderMap = new com.alibaba.fastjson2.JSONObject();
            } else {
                optOrderMap = com.alibaba.fastjson2.JSON.parseObject(record.getOptionOrder());
            }

            for (Map<String, Object> item : resultList) {
                Integer qType = (Integer) item.get("questionType");
                // 仅对单选(1)和多选(2)的选项打乱
                if (qType == null || (qType != 1 && qType != 2)) continue;

                String optionsStr = (String) item.get("options");
                if (optionsStr == null || optionsStr.isEmpty()) continue;

                com.alibaba.fastjson2.JSONArray opts = com.alibaba.fastjson2.JSON.parseArray(optionsStr);
                if (opts.size() <= 1) continue;

                String qIdStr = String.valueOf(item.get("questionId"));

                if (isFirstTime) {
                    // 生成随机索引映射
                    List<Integer> indices = new ArrayList<>();
                    for (int i = 0; i < opts.size(); i++) indices.add(i);
                    Collections.shuffle(indices);
                    com.alibaba.fastjson2.JSONArray idxArr = new com.alibaba.fastjson2.JSONArray();
                    idxArr.addAll(indices);
                    optOrderMap.put(qIdStr, idxArr);

                    // 按随机索引重排选项
                    com.alibaba.fastjson2.JSONArray shuffled = new com.alibaba.fastjson2.JSONArray();
                    for (int idx : indices) shuffled.add(opts.get(idx));
                    item.put("options", shuffled.toJSONString());
                } else {
                    // 按存储的映射重排
                    com.alibaba.fastjson2.JSONArray idxArr = optOrderMap.getJSONArray(qIdStr);
                    if (idxArr != null && idxArr.size() == opts.size()) {
                        com.alibaba.fastjson2.JSONArray restored = new com.alibaba.fastjson2.JSONArray();
                        for (int i = 0; i < idxArr.size(); i++) restored.add(opts.get(idxArr.getIntValue(i)));
                        item.put("options", restored.toJSONString());
                    }
                }
            }

            if (isFirstTime) {
                record.setOptionOrder(optOrderMap.toJSONString());
                needSave = true;
            }
        }

        if (needSave) {
            examRecordMapper.updateById(record);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("recordId", record.getId());
        data.put("paperName", record.getPaperName());
        data.put("duration", paper.getDuration());
        data.put("fastSubmitThreshold", threshold);
        data.put("questions", resultList);

        return Result.success(data);
    }


    @Override
    @Transactional
    public Result<?> submitExam(Long sessionId, Long userId, Map<String, Object> answers) {
        // 1. 查询考试记录
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .eq("user_id", userId)
                .eq("is_deleted", 0);
        ExamRecord record = examRecordMapper.selectOne(wrapper);
        if (record == null) {
            return Result.error("未找到考试记录");
        }
        if (record.getStatus() >= 2) {
            return Result.error("考试已提交");
        }
        // 【新增】检查是否超时
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session != null && LocalDateTime.now().isAfter(session.getEndTime())) {
            // 超时交卷，标记为超时自动交卷
            record.setSubmitType(2);
        } else {
            record.setSubmitType(1);
        }

        Long paperId = record.getPaperId();

        // 2. 查询试卷所有题目关联
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", paperId);
        List<PaperQuestion> pqList = paperQuestionMapper.selectList(pqWrapper);

        if (pqList == null || pqList.isEmpty()) {
            return Result.error("试卷没有题目");
        }

        Map<Long, PaperQuestion> pqMap = new HashMap<>();
        for (PaperQuestion pq : pqList) {
            pqMap.put(pq.getQuestionId(), pq);
        }

        // 3. 自动阅卷
        BigDecimal objectiveScore = BigDecimal.ZERO;
        Map<String, Object> scoreDetail = new HashMap<>();
        boolean hasSubjective = false;

        for (Map.Entry<String, Object> entry : answers.entrySet()) {
            Long questionId;
            try {
                questionId = Long.valueOf(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }

            String studentAnswer = entry.getValue() != null ? entry.getValue().toString() : "";

            PaperQuestion pq = pqMap.get(questionId);
            if (pq == null) {
                continue;
            }

            // 从快照中获取题目信息
            String questionSnapshot = pq.getQuestionSnapshot();
            if (questionSnapshot == null || questionSnapshot.isEmpty()) {
                continue;
            }

            Map<String, Object> snapshot;
            try {
                snapshot = JSON.parseObject(questionSnapshot);
            } catch (Exception e) {
                System.err.println("解析快照失败: " + e.getMessage());
                continue;
            }

            // 获取题型
            Object qTypeObj = snapshot.get("questionType");
            if (qTypeObj == null) {
                System.err.println("快照中没有 questionType: " + snapshot);
                continue;
            }
            int qType = ((Number) qTypeObj).intValue();

            // 获取正确答案
            Object correctAnswerObj = snapshot.get("answer");
            String correctAnswer = "";
            List<String> correctAnswerList = new ArrayList<>();

            if (correctAnswerObj instanceof java.util.List) {
                java.util.List list = (java.util.List) correctAnswerObj;
                for (Object item : list) {
                    correctAnswerList.add(item.toString());
                }
                correctAnswer = correctAnswerList.isEmpty() ? "" : correctAnswerList.get(0);
            } else if (correctAnswerObj != null) {
                correctAnswer = correctAnswerObj.toString();
                correctAnswerList.add(correctAnswer);
            }
            BigDecimal score = pq.getScore();

            boolean isCorrect = false;

            // 1=单选, 2=多选, 3=判断, 4=填空, 5=简答, 6=编程
            if (qType == 1) {
                // 单选：精确匹配（仅去除首尾空白）
                isCorrect = studentAnswer.trim().equals(correctAnswer.trim());
            } else if (qType == 3 || qType == 4) {
                // 判断/填空：忽略大小写与所有空格
                isCorrect = normalizeAnswer(studentAnswer).equalsIgnoreCase(normalizeAnswer(correctAnswer));
            } else if (qType == 2) {
                // 多选：无序匹配（排序后完整对比）
                Collections.sort(correctAnswerList);
                String sortedCorrect = String.join(",", correctAnswerList);
                // 去掉学生答案中的空格
                String cleanStudentAnswer = studentAnswer.replace(" ", "");
                isCorrect = compareMultiAnswer(cleanStudentAnswer, sortedCorrect);
            }  else {
                // 简答、编程 -> 主观题，需人工批阅
                hasSubjective = true;
                Map<String, Object> pendingDetail = new HashMap<>();
                pendingDetail.put("status", "pending");
                pendingDetail.put("studentAnswer", studentAnswer);
                pendingDetail.put("score", 0);
                scoreDetail.put(questionId.toString(), pendingDetail);
                continue;
            }
            // 多选时用完整列表
            String displayCorrectAnswer = qType == 2
                    ? String.join(", ", correctAnswerList)
                    : correctAnswer;
            if (isCorrect) {
                objectiveScore = objectiveScore.add(score);
                Map<String, Object> correctDetail = new HashMap<>();
                correctDetail.put("status", "correct");
                correctDetail.put("studentAnswer", studentAnswer);


                correctDetail.put("correctAnswer", displayCorrectAnswer);;
                correctDetail.put("score", score);
                scoreDetail.put(questionId.toString(), correctDetail);
            } else {
                Map<String, Object> wrongDetail = new HashMap<>();
                wrongDetail.put("status", "wrong");
                wrongDetail.put("studentAnswer", studentAnswer);
                wrongDetail.put("correctAnswer", displayCorrectAnswer);
                wrongDetail.put("score", 0);
                scoreDetail.put(questionId.toString(), wrongDetail);

                // 错题自动归集
                try {
                    WrongBook wrongBook = new WrongBook();
                    wrongBook.setUserId(userId);
                    wrongBook.setQuestionId(questionId);
                    wrongBook.setSubject((String) snapshot.get("subject"));
                    wrongBook.setExamName(record.getPaperName());
                    wrongBook.setIsMastered(0);
                    wrongBook.setIsDeleted(0);

                    // 包装为合法 JSON 格式
                    String jsonAnswer = "{\"answer\":\"" + studentAnswer.replace("\"", "\\\"") + "\"}";
                    wrongBook.setLastWrongAnswer(jsonAnswer);

                    wrongBookService.addWrongBook(wrongBook);
                } catch (Exception e) {
                    System.out.println("添加错题失败: " + e.getMessage());
                }
            }
        }

        // 4. 更新考试记录
        record.setFinalAnswers(JSON.toJSONString(answers));
        record.setScoreDetail(JSON.toJSONString(scoreDetail));
        record.setObjectiveScore(objectiveScore);
        record.setTotalScore(objectiveScore);
        record.setSubmitTime(LocalDateTime.now());

        if (hasSubjective) {
            record.setStatus(3); // 批阅中
        } else {
            record.setStatus(4); // 已完成
        }

        // 答题时间异常检测
        if (session != null && record.getStartTime() != null) {
            long actualSeconds = java.time.Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
            Paper paper = paperMapper.selectById(paperId);
            if (paper != null && paper.getDuration() != null) {
                long totalSeconds = paper.getDuration() * 60L;
                Integer thresholdPercent = session.getFastSubmitThreshold();
                if (thresholdPercent == null) thresholdPercent = 20;
                long minSeconds = totalSeconds * thresholdPercent / 100;

                if (actualSeconds < minSeconds && actualSeconds > 0) {
                    // 合并写入异常日志，避免覆盖已有记录
                    com.alibaba.fastjson2.JSONArray logs;
                    String existing = record.getCheatLogs();
                    if (existing != null && !existing.trim().isEmpty()) {
                        try {
                            logs = com.alibaba.fastjson2.JSON.parseArray(existing);
                        } catch (Exception e) {
                            logs = new com.alibaba.fastjson2.JSONArray();
                        }
                    } else {
                        logs = new com.alibaba.fastjson2.JSONArray();
                    }
                    com.alibaba.fastjson2.JSONObject entry = new com.alibaba.fastjson2.JSONObject();
                    entry.put("type", "fast_submit");
                    entry.put("time", actualSeconds);
                    entry.put("threshold", minSeconds);
                    logs.add(entry);
                    record.setCheatLogs(logs.toJSONString());
                }
            }
        }
        examRecordMapper.updateById(record);

        // 5. 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("objectiveScore", objectiveScore);
        data.put("totalScore", record.getTotalScore());
        data.put("status", record.getStatus());

        return Result.success("提交成功", data);
    }

    @Override
    public Result<?> myExams(Long userId) {
        // 1. 查询用户的所有考试记录
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");
        List<ExamRecord> list = examRecordMapper.selectList(wrapper);

        if (list == null || list.isEmpty()) {
            return Result.success(list);
        }

        // 2. 收集所有场次ID
        List<Long> sessionIds = list.stream()
                .map(ExamRecord::getSessionId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量查询场次名称
        Map<Long, String> sessionNameMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<ExamSession> sessions = examSessionMapper.selectBatchIds(sessionIds);
            for (ExamSession s : sessions) {
                sessionNameMap.put(s.getId(), s.getSessionName());
            }
        }

        // 4. 批量查询每场次的所有已完成记录（status=4），按总分降序
        Map<Long, List<ExamRecord>> sessionRankMap = new HashMap<>();
        for (Long sessionId : sessionIds) {
            QueryWrapper<ExamRecord> sessionWrapper = new QueryWrapper<>();
            sessionWrapper.eq("session_id", sessionId)
                    .eq("is_deleted", 0)
                    .eq("status", 4)
                    .orderByDesc("total_score");
            List<ExamRecord> sessionRecords = examRecordMapper.selectList(sessionWrapper);
            sessionRankMap.put(sessionId, sessionRecords);
        }

        // 5. 为每个记录计算排名
        for (ExamRecord record : list) {
            List<ExamRecord> sessionRecords = sessionRankMap.get(record.getSessionId());
            if (sessionRecords != null && !sessionRecords.isEmpty()) {
                int rank = 1;
                for (ExamRecord r : sessionRecords) {
                    if (r.getTotalScore() != null && record.getTotalScore() != null) {
                        if (r.getTotalScore().compareTo(record.getTotalScore()) > 0) {
                            rank++;
                        }
                    }
                }
                record.setRank(rank);
            } else {
                record.setRank(0);
            }
        }

        // 6. 为每条记录设置考试名称和时长
        for (ExamRecord record : list) {
            // 设置考试名称
            String sessionName = sessionNameMap.get(record.getSessionId());
            record.setSessionName(sessionName != null ? sessionName : record.getPaperName());
            // 设置异常标记
            record.setAbnormal(record.getCheatLogs() != null && !record.getCheatLogs().isEmpty());
            // 设置时长
            if (record.getPaperId() != null) {
                Paper paper = paperMapper.selectById(record.getPaperId());
                if (paper != null) {
                    record.setDuration(paper.getDuration());
                }
            }
        }

        return Result.success(list);
    }

    /**
     * 比较多选答案（排序后对比）
     */
    private boolean compareMultiAnswer(String studentAnswer, String correctAnswer) {
        try {
            List<String> studentList = parseAnswerList(studentAnswer);
            List<String> correctList = parseAnswerList(correctAnswer);
            if (studentList.size() != correctList.size()) {
                return false;
            }
            Collections.sort(studentList);
            Collections.sort(correctList);
            return studentList.equals(correctList);
        } catch (Exception e) {
            return studentAnswer.equalsIgnoreCase(correctAnswer);
        }
    }

    /**
     * 解析答案列表（支持 JSON 数组或逗号分隔字符串）
     */
    private List<String> parseAnswerList(String answer) {
        if (answer == null || answer.isEmpty()) {
            return new ArrayList<>();
        }
        // 去掉所有空格
        String cleanAnswer = answer.replace(" ", "");
        if (cleanAnswer.startsWith("[") && cleanAnswer.endsWith("]")) {
            // 去掉首尾括号
            cleanAnswer = cleanAnswer.substring(1, cleanAnswer.length() - 1);
        }
        // 按逗号分割，去掉空元素
        return new ArrayList<>(Arrays.asList(cleanAnswer.split(",")));
    }

    /**
     * 归一化答案：去除所有空白字符（用于判断/填空忽略大小写与空格比较）
     */
    private String normalizeAnswer(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "");
    }
}
