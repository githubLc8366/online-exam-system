package com.hbnu.service.impl;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.Result;
import com.hbnu.dto.PaperCreateDTO;
import com.hbnu.mapper.PaperMapper;
import com.hbnu.mapper.PaperQuestionMapper;
import com.hbnu.mapper.QuestionMapper;
import com.hbnu.pojo.Paper;
import com.hbnu.pojo.PaperQuestion;
import com.hbnu.pojo.Question;
import com.hbnu.service.PaperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hbnu.common.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import com.hbnu.dto.PaperCreateDTO.RandomConfigDTO;
import com.hbnu.dto.PaperCreateDTO.DifficultyRatioDTO;


/**
 * 试卷 Service 实现类
 */
@Slf4j
@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createPaper(PaperCreateDTO dto, Long creatorId) {
        // 1. 名称查重（去除空格）
        QueryWrapper<Paper> nameWrapper = new QueryWrapper<>();
        nameWrapper.eq("paper_name", dto.getPaperName().trim());
        Long count = paperMapper.selectCount(nameWrapper);
        if (count > 0) {
            return Result.error("试卷名称重复");
        }

        // 2. 处理题目列表（根据组卷模式获取题目）
        List<PaperCreateDTO.PaperQuestionItemDTO> questions = null;

        if ("random".equals(dto.getMode()) && dto.getRandomConfig() != null) {
            // 随机组卷：根据配置自动生成题目列表
            try {
                questions = generateQuestionsByRandomConfig(dto);
            } catch (BusinessException e) {
                return Result.error(e.getMessage());
            }
            if (questions == null || questions.isEmpty()) {
                return Result.error("题库中题目不足，无法满足随机组卷配置，请调整题目数量或新增题目");
            }
        } else {
            // 手动组卷：使用前端传递的题目列表
            questions = dto.getQuestions();
            if (questions == null || questions.isEmpty()) {
                return Result.error("请至少选择一道题目");
            }
        }

        // 3. 自动计算总分：累加所有题目的分值
        BigDecimal totalScore = BigDecimal.ZERO;
        for (PaperCreateDTO.PaperQuestionItemDTO item : questions) {
            totalScore = totalScore.add(item.getScore() != null ? item.getScore() : BigDecimal.ZERO);
        }

        // 4. 保存试卷基本信息
        Paper paper = new Paper();
        paper.setPaperName(dto.getPaperName().trim());
        paper.setTotalScore(totalScore);
        // 及格线 = 总分的60%
        // 及格线：教师填了就用他的，没填或0就用60%
        BigDecimal passScore = dto.getPassScore();
        if (passScore == null || passScore.compareTo(BigDecimal.ZERO) <= 0) {
            passScore = totalScore.multiply(BigDecimal.valueOf(0.6));
        }
        paper.setPassScore(passScore);

        // 时长：教师填了就用他的，没填默认120
        paper.setDuration(dto.getDuration() != null ? dto.getDuration() : 120);
        paper.setSubject(dto.getSubject() != null ? dto.getSubject().trim() : null);
        paper.setRemark(dto.getRemark());
        paper.setCreatorId(creatorId);
        paper.setQuestionCount(questions.size());
        paper.setStatus(0);  // 草稿状态
        paper.setMode(dto.getMode());

        paperMapper.insert(paper);

        // 5. 批量插入试卷题目关联（包含快照）
        List<PaperQuestion> pqList = new ArrayList<>();
        List<Long> questionIds = new ArrayList<>();

        // 收集题目ID - 使用 questions
        for (PaperCreateDTO.PaperQuestionItemDTO item : questions) {
            questionIds.add(item.getQuestionId());
        }

        // 6. 检查题目列表是否为空
        if (questionIds.isEmpty()) {
            return Result.error("所选科目题库不足");
        }

        // 查询所有题目信息
        List<Question> questionList = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questionList) {
            questionMap.put(q.getId(), q);
        }

        // 构建试卷题目关联 - 使用 questions（重要！不能再用 dto.getQuestions()）
        for (PaperCreateDTO.PaperQuestionItemDTO item : questions) {  // ← 改这里
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(item.getQuestionId());
            pq.setQuestionOrder(item.getQuestionOrder());
            pq.setScore(item.getScore());
            pq.setCreateTime(LocalDateTime.now());

            // 生成题目快照
            Question question = questionMap.get(item.getQuestionId());
            if (question != null) {
                com.alibaba.fastjson2.JSONObject snapshot = new com.alibaba.fastjson2.JSONObject();
                snapshot.put("id", question.getId());
                snapshot.put("questionType", question.getQuestionType());
                snapshot.put("content", question.getContent() != null ? question.getContent() : "");
                snapshot.put("analysis", question.getAnalysis() != null ? question.getAnalysis() : "");

                // ========== 处理 answer：统一转为 JSON 数组存入 ==========
                String answer = question.getAnswer();
                if (answer != null && !answer.isEmpty()) {
                    String cleanAnswer = answer.trim();
                    // 如果已经是 JSON 数组格式，直接解析
                    if (cleanAnswer.startsWith("[")) {
                        try {
                            snapshot.put("answer", JSON.parse(cleanAnswer));
                        } catch (Exception e) {
                            // 解析失败，包装为数组
                            com.alibaba.fastjson2.JSONArray arr = new com.alibaba.fastjson2.JSONArray();
                            arr.add(cleanAnswer);
                            snapshot.put("answer", arr);
                        }
                    } else {
                        // 普通字符串，包装为数组
                        com.alibaba.fastjson2.JSONArray arr = new com.alibaba.fastjson2.JSONArray();
                        arr.add(cleanAnswer);
                        snapshot.put("answer", arr);
                    }
                }

                // ========== 处理 options ==========
                String options = question.getOptions();
                if (options != null && !options.isEmpty()) {
                    String cleanOptions = options.trim();
                    if (cleanOptions.startsWith("[")) {
                        try {
                            snapshot.put("options", JSON.parse(cleanOptions));
                        } catch (Exception e) {
                            snapshot.put("options", new com.alibaba.fastjson2.JSONArray());
                        }
                    } else {
                        snapshot.put("options", new com.alibaba.fastjson2.JSONArray());
                    }
                } else {
                    snapshot.put("options", new com.alibaba.fastjson2.JSONArray());
                }

                // ========== 处理 categoryTags ==========
                String tags = question.getCategoryTags();
                if (tags != null && !tags.isEmpty()) {
                    String cleanTags = tags.trim();
                    if (cleanTags.startsWith("[")) {
                        try {
                            snapshot.put("categoryTags", JSON.parse(cleanTags));
                        } catch (Exception e) {
                            snapshot.put("categoryTags", new com.alibaba.fastjson2.JSONArray());
                        }
                    } else {
                        com.alibaba.fastjson2.JSONArray tagArr = new com.alibaba.fastjson2.JSONArray();
                        tagArr.add(cleanTags);
                        snapshot.put("categoryTags", tagArr);
                    }
                } else {
                    snapshot.put("categoryTags", new com.alibaba.fastjson2.JSONArray());
                }

                snapshot.put("difficulty", question.getDifficulty());
                snapshot.put("subject", question.getSubject() != null ? question.getSubject() : "");
                snapshot.put("score", question.getScore());

                pq.setQuestionSnapshot(snapshot.toJSONString());
            }

            pqList.add(pq);
        }

        // 批量保存
        for (PaperQuestion pq : pqList) {
            paperQuestionMapper.insert(pq);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paperId", paper.getId());
        data.put("totalScore", totalScore);
        return Result.success("创建成功", data);
    }

    @Override
    public Result<?> getDetail(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null || paper.getIsDeleted() == 1) {  // 【新增】
            return Result.error("试卷不存在");
        }

        QueryWrapper<PaperQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("paper_id", id).orderByAsc("question_order");
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(wrapper);

        List<Map<String, Object>> questionList = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            Map<String, Object> q = new HashMap<>();

            String snapshotJson = pq.getQuestionSnapshot();
            if (snapshotJson != null && !snapshotJson.isEmpty()) {
                try {
                    // 使用 Fastjson 解析
                    com.alibaba.fastjson2.JSONObject snapshot = JSON.parseObject(snapshotJson);

                    q.put("questionId", snapshot.get("id"));

                    // 题型
                    Object qTypeObj = snapshot.get("questionType");
                    q.put("questionType", qTypeObj != null ? ((Number) qTypeObj).intValue() : 5);

                    // 内容
                    q.put("content", snapshot.getString("content") != null ? snapshot.getString("content") : "题目内容缺失");
                    q.put("score", pq.getScore());
                    q.put("questionOrder", pq.getQuestionOrder());

                    // options：直接获取，已经是 JSONArray
                    Object optionsObj = snapshot.get("options");
                    if (optionsObj instanceof com.alibaba.fastjson2.JSONArray) {
                        q.put("options", optionsObj);
                    } else if (optionsObj instanceof String) {
                        try {
                            q.put("options", JSON.parseArray((String) optionsObj));
                        } catch (Exception e) {
                            q.put("options", new ArrayList<>());
                        }
                    } else {
                        q.put("options", new ArrayList<>());
                    }

                    // answer
                    Object answerObj = snapshot.get("answer");
                    if (answerObj != null) {
                        q.put("answer", answerObj);
                    }

                    // 知识点标签（预览展示用）
                    Object tagsObj = snapshot.get("categoryTags");
                    if (tagsObj instanceof com.alibaba.fastjson2.JSONArray) {
                        q.put("categoryTags", tagsObj);
                    } else if (tagsObj instanceof String) {
                        try {
                            q.put("categoryTags", JSON.parseArray((String) tagsObj));
                        } catch (Exception e) {
                            q.put("categoryTags", new ArrayList<>());
                        }
                    } else {
                        q.put("categoryTags", new ArrayList<>());
                    }

                    q.put("analysis", snapshot.getString("analysis"));
                    q.put("difficulty", snapshot.get("difficulty"));

                } catch (Exception e) {
                    log.error("解析题目快照失败: {}，快照内容: {}", e.getMessage(),
                            snapshotJson.substring(0, Math.min(200, snapshotJson.length())));
                    q.put("questionId", pq.getQuestionId());
                    q.put("content", "题目加载失败");
                    q.put("questionType", 5);
                    q.put("score", pq.getScore());
                    q.put("options", new ArrayList<>());
                }
            } else {
                q.put("questionId", pq.getQuestionId());
                q.put("content", "题目快照缺失");
                q.put("questionType", 5);
                q.put("score", pq.getScore());
                q.put("options", new ArrayList<>());
            }

            questionList.add(q);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", questionList);

        return Result.success(data);
    }
    @Override
    public Result<IPage<Paper>> pageList(Integer page, Integer size, String subject, Integer status) {
        Page<Paper> pageParam = new Page<>(page, size);
        QueryWrapper<Paper> queryWrapper = new QueryWrapper<>();

        // 【修复】过滤已删除的记录
        queryWrapper.eq("is_deleted", 0);

        if (subject != null && !subject.isEmpty()) {
            queryWrapper.eq("subject", subject);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("create_time");

        IPage<Paper> result = paperMapper.selectPage(pageParam, queryWrapper);
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(Long id) {
        // 1. 检查试卷是否存在
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        // 2. 级联删除：先删除 exam_paper_question 关联表数据
        QueryWrapper<PaperQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("paper_id", id);
        paperQuestionMapper.delete(wrapper);

        // 3. 删除 exam_paper 记录
        paperMapper.deleteById(id);

        return Result.success("删除成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updatePaper(PaperCreateDTO dto, Long creatorId) {
        // 1. 检查试卷是否存在
        Paper existingPaper = paperMapper.selectById(dto.getPaperId());
        if (existingPaper == null) {
            return Result.error("试卷不存在");
        }

        // 2. 名称查重（排除自己）
        QueryWrapper<Paper> nameWrapper = new QueryWrapper<>();
        nameWrapper.eq("paper_name", dto.getPaperName().trim())
                .ne("id", dto.getPaperId());
        Long count = paperMapper.selectCount(nameWrapper);
        if (count > 0) {
            return Result.error("试卷名称重复");
        }

        // 3. 处理题目列表（根据组卷模式获取题目）
        List<PaperCreateDTO.PaperQuestionItemDTO> questions = null;

        if ("random".equals(dto.getMode()) && dto.getRandomConfig() != null) {
            // 随机组卷：根据配置自动生成题目列表
            try {
                questions = generateQuestionsByRandomConfig(dto);
            } catch (BusinessException e) {
                return Result.error(e.getMessage());
            }
            if (questions == null || questions.isEmpty()) {
                return Result.error("题库中题目不足，无法满足随机组卷配置");
            }
        } else {
            // 手动组卷：使用前端传递的题目列表
            questions = dto.getQuestions();
            if (questions == null || questions.isEmpty()) {
                return Result.error("请至少选择一道题目");
            }
        }

        // 4. 自动计算总分
        BigDecimal totalScore = BigDecimal.ZERO;
        for (PaperCreateDTO.PaperQuestionItemDTO item : questions) {
            totalScore = totalScore.add(item.getScore() != null ? item.getScore() : BigDecimal.ZERO);
        }

        // 5. 更新试卷基本信息
        existingPaper.setPaperName(dto.getPaperName().trim());
        existingPaper.setTotalScore(totalScore);
        // 及格线
        BigDecimal passScore = dto.getPassScore();
        if (passScore == null || passScore.compareTo(BigDecimal.ZERO) <= 0) {
            passScore = totalScore.multiply(BigDecimal.valueOf(0.6));
        }
        existingPaper.setPassScore(passScore);

        // 时长
        existingPaper.setDuration(dto.getDuration() != null ? dto.getDuration() : 120);
        existingPaper.setSubject(dto.getSubject() != null ? dto.getSubject().trim() : null);
        existingPaper.setRemark(dto.getRemark());
        existingPaper.setQuestionCount(questions.size());
        existingPaper.setMode(dto.getMode());
        existingPaper.setUpdateTime(LocalDateTime.now());

        paperMapper.updateById(existingPaper);

        // 6. 删除旧的试卷题目关联
        QueryWrapper<PaperQuestion> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("paper_id", dto.getPaperId());
        paperQuestionMapper.delete(deleteWrapper);

        // 7. 查询所有题目信息
        List<Long> questionIds = new ArrayList<>();
        for (PaperCreateDTO.PaperQuestionItemDTO item : questions) {
            questionIds.add(item.getQuestionId());
        }

        List<Question> questionList = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questionList) {
            questionMap.put(q.getId(), q);
        }

        // 8. 批量插入新的试卷题目关联
        List<PaperQuestion> pqList = new ArrayList<>();
        for (PaperCreateDTO.PaperQuestionItemDTO item : questions) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(existingPaper.getId());
            pq.setQuestionId(item.getQuestionId());
            pq.setQuestionOrder(item.getQuestionOrder());
            pq.setScore(item.getScore());
            pq.setCreateTime(LocalDateTime.now());

            // 生成题目快照
            Question question = questionMap.get(item.getQuestionId());
            if (question != null) {
                com.alibaba.fastjson2.JSONObject snapshot = new com.alibaba.fastjson2.JSONObject();
                snapshot.put("id", question.getId());
                snapshot.put("questionType", question.getQuestionType());
                snapshot.put("content", question.getContent() != null ? question.getContent() : "");
                snapshot.put("analysis", question.getAnalysis() != null ? question.getAnalysis() : "");

                // ========== 处理 answer：统一转为 JSON 数组存入 ==========
                String answer = question.getAnswer();
                if (answer != null && !answer.isEmpty()) {
                    String cleanAnswer = answer.trim();
                    // 如果已经是 JSON 数组格式，直接解析
                    if (cleanAnswer.startsWith("[")) {
                        try {
                            snapshot.put("answer", JSON.parse(cleanAnswer));
                        } catch (Exception e) {
                            // 解析失败，包装为数组
                            com.alibaba.fastjson2.JSONArray arr = new com.alibaba.fastjson2.JSONArray();
                            arr.add(cleanAnswer);
                            snapshot.put("answer", arr);
                        }
                    } else {
                        // 普通字符串，包装为数组
                        com.alibaba.fastjson2.JSONArray arr = new com.alibaba.fastjson2.JSONArray();
                        arr.add(cleanAnswer);
                        snapshot.put("answer", arr);
                    }
                }

                // ========== 处理 options ==========
                String options = question.getOptions();
                if (options != null && !options.isEmpty()) {
                    String cleanOptions = options.trim();
                    if (cleanOptions.startsWith("[")) {
                        try {
                            snapshot.put("options", JSON.parse(cleanOptions));
                        } catch (Exception e) {
                            snapshot.put("options", new com.alibaba.fastjson2.JSONArray());
                        }
                    } else {
                        snapshot.put("options", new com.alibaba.fastjson2.JSONArray());
                    }
                } else {
                    snapshot.put("options", new com.alibaba.fastjson2.JSONArray());
                }

                // ========== 处理 categoryTags ==========
                String tags = question.getCategoryTags();
                if (tags != null && !tags.isEmpty()) {
                    String cleanTags = tags.trim();
                    if (cleanTags.startsWith("[")) {
                        try {
                            snapshot.put("categoryTags", JSON.parse(cleanTags));
                        } catch (Exception e) {
                            snapshot.put("categoryTags", new com.alibaba.fastjson2.JSONArray());
                        }
                    } else {
                        com.alibaba.fastjson2.JSONArray tagArr = new com.alibaba.fastjson2.JSONArray();
                        tagArr.add(cleanTags);
                        snapshot.put("categoryTags", tagArr);
                    }
                } else {
                    snapshot.put("categoryTags", new com.alibaba.fastjson2.JSONArray());
                }

                snapshot.put("difficulty", question.getDifficulty());
                snapshot.put("subject", question.getSubject() != null ? question.getSubject() : "");
                snapshot.put("score", question.getScore());

                pq.setQuestionSnapshot(snapshot.toJSONString());
            }

            pqList.add(pq);
        }

        for (PaperQuestion pq : pqList) {
            paperQuestionMapper.insert(pq);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paperId", existingPaper.getId());
        data.put("totalScore", totalScore);
        return Result.success("更新成功", data);
    }

    /**
     * 根据随机组卷配置自动抽题
     *
     * @param dto 试卷创建请求
     * @return 题目列表（包含顺序和分值）
     */
    /**
     * 根据随机组卷配置自动抽题
     */
    private List<PaperCreateDTO.PaperQuestionItemDTO> generateQuestionsByRandomConfig(PaperCreateDTO dto) {
        List<PaperCreateDTO.PaperQuestionItemDTO> result = new ArrayList<>();
        RandomConfigDTO rc = dto.getRandomConfig();

        if (rc == null) {
            return result;
        }

        int order = 1;
        List<String> insufficientTypes = new ArrayList<>();

        // 单选题
        if (rc.getSingleCount() != null && rc.getSingleCount() > 0) {
            List<PaperCreateDTO.PaperQuestionItemDTO> items = selectQuestionsByTypeAndDifficulty(
                    1, rc.getSingleCount(), rc.getSingleScore(), dto.getSubject(), rc, order);
            if (items.size() < rc.getSingleCount()) {
                insufficientTypes.add("单选题（需要 " + rc.getSingleCount() + " 道，实际 " + items.size() + " 道）");
            }
            result.addAll(items);
            order += items.size();
        }

        // 多选题
        if (rc.getMultipleCount() != null && rc.getMultipleCount() > 0) {
            List<PaperCreateDTO.PaperQuestionItemDTO> items = selectQuestionsByTypeAndDifficulty(
                    2, rc.getMultipleCount(), rc.getMultipleScore(), dto.getSubject(), rc, order);
            if (items.size() < rc.getMultipleCount()) {
                insufficientTypes.add("多选题（需要 " + rc.getMultipleCount() + " 道，实际 " + items.size() + " 道）");
            }
            result.addAll(items);
            order += items.size();
        }

        // 判断题
        if (rc.getJudgeCount() != null && rc.getJudgeCount() > 0) {
            List<PaperCreateDTO.PaperQuestionItemDTO> items = selectQuestionsByTypeAndDifficulty(
                    3, rc.getJudgeCount(), rc.getJudgeScore(), dto.getSubject(), rc, order);
            if (items.size() < rc.getJudgeCount()) {
                insufficientTypes.add("判断题（需要 " + rc.getJudgeCount() + " 道，实际 " + items.size() + " 道）");
            }
            result.addAll(items);
            order += items.size();
        }

        // 填空题
        if (rc.getFillCount() != null && rc.getFillCount() > 0) {
            List<PaperCreateDTO.PaperQuestionItemDTO> items = selectQuestionsByTypeAndDifficulty(
                    4, rc.getFillCount(), rc.getFillScore(), dto.getSubject(), rc, order);
            if (items.size() < rc.getFillCount()) {
                insufficientTypes.add("填空题（需要 " + rc.getFillCount() + " 道，实际 " + items.size() + " 道）");
            }
            result.addAll(items);
            order += items.size();
        }

        // 简答题
        if (rc.getShortCount() != null && rc.getShortCount() > 0) {
            List<PaperCreateDTO.PaperQuestionItemDTO> items = selectQuestionsByTypeAndDifficulty(
                    5, rc.getShortCount(), rc.getShortScore(), dto.getSubject(), rc, order);
            if (items.size() < rc.getShortCount()) {
                insufficientTypes.add("简答题（需要 " + rc.getShortCount() + " 道，实际 " + items.size() + " 道）");
            }
            result.addAll(items);
            order += items.size();
        }

        // 编程题
        if (rc.getProgramCount() != null && rc.getProgramCount() > 0) {
            List<PaperCreateDTO.PaperQuestionItemDTO> items = selectQuestionsByTypeAndDifficulty(
                    6, rc.getProgramCount(), rc.getProgramScore(), dto.getSubject(), rc, order);
            if (items.size() < rc.getProgramCount()) {
                insufficientTypes.add("编程题（需要 " + rc.getProgramCount() + " 道，实际 " + items.size() + " 道）");
            }
            result.addAll(items);
        }

        if (!insufficientTypes.isEmpty()) {
            String errorMsg = "题库中题目不足，无法满足随机组卷配置，请调整：\n" + String.join("\n", insufficientTypes);
            log.warn(errorMsg);
            throw new BusinessException(errorMsg);
        }

        log.info("随机组卷生成题目数量: 配置总数={}, 实际生成={}",
                rc.getSingleCount() + rc.getMultipleCount() + rc.getJudgeCount() +
                        rc.getFillCount() + rc.getShortCount() + rc.getProgramCount(), result.size());

        return result;
    }
    /**
     * 按题型、数量、难度比例从题库中随机抽题
     *
     * @param questionType 题型（1=单选,2=多选,3=判断,4=填空,5=简答,6=编程）
     * @param count        需要抽取的数量
     * @param score        每题分值
     * @param subject      科目
     * @param ratio        难度比例
     * @param startOrder   起始顺序
     * @return 题目列表
     */
    /**
     * 按题型、数量、难度比例从题库中随机抽题
     */
    /**
     * 按题型、数量、难度比例、知识点比例从题库中随机抽题
     */
    private List<PaperCreateDTO.PaperQuestionItemDTO> selectQuestionsByTypeAndDifficulty(
            Integer questionType, Integer count, BigDecimal score,
            String subject, RandomConfigDTO rc, int startOrder) {

        List<PaperCreateDTO.PaperQuestionItemDTO> result = new ArrayList<>();
        if (count == null || count == 0) {
            return result;
        }

        DifficultyRatioDTO ratio = rc.getDifficultyRatio();

        // 向下取整，余数归中等难度
        int easyCount = count * ratio.getEasy() / 100;
        int hardCount = count * ratio.getHard() / 100;
        int mediumCount = count - easyCount - hardCount;

        if (easyCount < 0) easyCount = 0;
        if (mediumCount < 0) mediumCount = 0;
        if (hardCount < 0) hardCount = 0;

        log.info("随机抽题 - 题型:{}, 总数:{}, 易:{}, 中:{}, 难:{}, 科目:{}",
                questionType, count, easyCount, mediumCount, hardCount, subject);

        // 抽取易难度题目
        result.addAll(randomSelectQuestions(questionType, 1, easyCount, subject, score, startOrder));
        // 抽取中难度题目
        result.addAll(randomSelectQuestions(questionType, 3, mediumCount, subject, score, startOrder + result.size()));
        // 抽取难难度题目
        result.addAll(randomSelectQuestions(questionType, 5, hardCount, subject, score, startOrder + result.size()));

        // 按难度分配后不够，从该题型全部题目中补抽
        int shortage = count - result.size();
        if (shortage > 0) {
            log.info("题型{}按难度分配后不足，缺{}道，从全部难度补抽", questionType, shortage);

            Set<Long> alreadySelected = new HashSet<>();
            for (PaperCreateDTO.PaperQuestionItemDTO item : result) {
                alreadySelected.add(item.getQuestionId());
            }

            QueryWrapper<Question> wrapper = new QueryWrapper<>();
            wrapper.eq("question_type", questionType);
            wrapper.eq("status", 1);
            wrapper.eq("is_deleted", 0);
            if (subject != null && !subject.isEmpty()) {
                wrapper.eq("subject", subject);
            }
            if (!alreadySelected.isEmpty()) {
                wrapper.notIn("id", alreadySelected);
            }
            wrapper.last("ORDER BY RAND() LIMIT " + shortage);

            List<Question> supplementQuestions = questionMapper.selectList(wrapper);
            for (Question q : supplementQuestions) {
                PaperCreateDTO.PaperQuestionItemDTO item = new PaperCreateDTO.PaperQuestionItemDTO();
                item.setQuestionId(q.getId());
                item.setQuestionOrder(startOrder + result.size());
                item.setScore(score);
                result.add(item);
            }
        }

        // ===== 新增：知识点比例过滤 =====
        Map<String, Integer> knowledgeRatio = rc.getKnowledgeRatio();
        if (knowledgeRatio != null && !knowledgeRatio.isEmpty()) {
            // 计算有效比例
            int totalPercent = knowledgeRatio.values().stream().mapToInt(Integer::intValue).sum();
            if (totalPercent > 0) {
                Map<String, Integer> knowledgeNeeded = new HashMap<>();
                for (Map.Entry<String, Integer> entry : knowledgeRatio.entrySet()) {
                    int needed = count * entry.getValue() / totalPercent;
                    if (needed > 0) {
                        knowledgeNeeded.put(entry.getKey(), needed);
                    }
                }

                // 先收集匹配知识点的题目
                List<PaperCreateDTO.PaperQuestionItemDTO> filtered = new ArrayList<>();
                List<PaperCreateDTO.PaperQuestionItemDTO> unmatched = new ArrayList<>();
                Set<Long> usedIds = new HashSet<>();

                for (PaperCreateDTO.PaperQuestionItemDTO item : result) {
                    Long qid = item.getQuestionId();
                    if (usedIds.contains(qid)) continue;

                    Question q = questionMapper.selectById(qid);
                    if (q != null && q.getCategoryTags() != null) {
                        try {
                            List<String> tags = JSON.parseArray(q.getCategoryTags(), String.class);
                            boolean matched = false;
                            for (String tag : tags) {
                                if (knowledgeNeeded.containsKey(tag) && knowledgeNeeded.get(tag) > 0) {
                                    knowledgeNeeded.put(tag, knowledgeNeeded.get(tag) - 1);
                                    filtered.add(item);
                                    usedIds.add(qid);
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) {
                                unmatched.add(item);
                            }
                        } catch (Exception e) {
                            unmatched.add(item);
                        }
                    } else {
                        unmatched.add(item);
                    }
                }

                // 知识点匹配的题目不够，用未匹配的补足
                while (filtered.size() < count && !unmatched.isEmpty()) {
                    filtered.add(unmatched.remove(0));
                }

                result = filtered;
            }
        }

        return result;
    }

    @Override
    public Result<Map<String, Object>> getQuestionStats(String subject) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 题型列表：1=单选, 2=多选, 3=判断, 4=填空, 5=简答, 6=编程
        int[] types = {1, 2, 3, 4, 5, 6};
        String[] typeNames = {"single", "multiple", "judge", "fill", "short", "program"};

        for (int i = 0; i < types.length; i++) {
            QueryWrapper<Question> wrapper = new QueryWrapper<>();
            wrapper.eq("question_type", types[i]);
            wrapper.eq("status", 1);
            wrapper.eq("is_deleted", 0);
            if (subject != null && !subject.isEmpty()) {
                wrapper.eq("subject", subject);
            }
            Long count = questionMapper.selectCount(wrapper);

            Map<String, Object> typeStat = new HashMap<>();
            typeStat.put("count", count);
            typeStat.put("typeName", typeNames[i]);
            stats.put(typeNames[i], typeStat);
        }

        return Result.success(stats);
    }

    /**
     * 从题库中随机抽取指定数量、题型、难度的题目
     *
     * @param questionType 题型
     * @param difficulty   难度（1=易,3=中,5=难）
     * @param count        抽取数量
     * @param subject      科目
     * @param score        每题分值
     * @param startOrder   起始顺序
     * @return 题目列表
     */
    private List<PaperCreateDTO.PaperQuestionItemDTO> randomSelectQuestions(
            Integer questionType, Integer difficulty, Integer count,
            String subject, BigDecimal score, int startOrder) {

        List<PaperCreateDTO.PaperQuestionItemDTO> result = new ArrayList<>();

        if (count == null || count <= 0) {
            return result;
        }

        // 构建查询条件
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("question_type", questionType);
        wrapper.eq("difficulty", difficulty);
        wrapper.eq("status", 1);
        wrapper.eq("is_deleted", 0);

        if (subject != null && !subject.isEmpty()) {
            wrapper.eq("subject", subject);
        }

        // 查询符合条件的题目总数
        Long total = questionMapper.selectCount(wrapper);

        if (total < count) {
            // 题目不足时返回所有可用题目
            List<Question> questions = questionMapper.selectList(wrapper);
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                PaperCreateDTO.PaperQuestionItemDTO item = new PaperCreateDTO.PaperQuestionItemDTO();
                item.setQuestionId(q.getId());
                item.setQuestionOrder(startOrder + i);
                item.setScore(score);
                result.add(item);
            }
        } else {
            // 随机抽取指定数量
            wrapper.last("ORDER BY RAND() LIMIT " + count);
            List<Question> questions = questionMapper.selectList(wrapper);

            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                PaperCreateDTO.PaperQuestionItemDTO item = new PaperCreateDTO.PaperQuestionItemDTO();
                item.setQuestionId(q.getId());
                item.setQuestionOrder(startOrder + i);
                item.setScore(score);
                result.add(item);
            }
        }

        return result;
    }
}
