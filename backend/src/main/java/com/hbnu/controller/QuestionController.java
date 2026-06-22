package com.hbnu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.pojo.Question;
import com.hbnu.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.hbnu.annotation.Log;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import com.alibaba.fastjson2.JSON;
import java.util.*;

/**
 * 题目管理控制器
 */
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 分页查询题目列表
     *
     * @param page         当前页
     * @param size         每页大小
     * @param questionType 题型
     * @param subject      科目
     * @param difficulty   难度
     * @return Result<IPage<Question>>
     */
    @GetMapping("/list")
    public Result<IPage<Question>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer difficulty) {
        return questionService.pageList(page, size, questionType, subject, difficulty);
    }

    /**
     * 新增题目
     *
     * @param question 题目信息
     * @return Result
     */
    @Log(module = "题库管理", action = "新增题目")
    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long creatorId = getCurrentUserId(request);

        Question question = new Question();
        question.setCreatorId(creatorId);
        question.setContent(safeString(body.get("content")));
        question.setSubject(safeString(body.get("subject")));
        question.setAnalysis(safeString(body.get("analysis")));

        // 题型
        Object typeObj = body.get("questionType");
        question.setQuestionType(typeObj != null ? Integer.parseInt(typeObj.toString()) : 1);

        // 答案：可能是字符串或数组
        Object answerObj = body.get("answer");
        if (answerObj instanceof List) {
            question.setAnswer(JSON.toJSONString(answerObj));
        } else if (answerObj != null) {
            question.setAnswer("[\"" + answerObj.toString() + "\"]");
        } else {
            question.setAnswer("[\"\"]");
        }

        // 选项
        Object optionsObj = body.get("options");
        if (optionsObj instanceof List) {
            question.setOptions(JSON.toJSONString(optionsObj));
        } else if (optionsObj != null && !optionsObj.toString().isEmpty()) {
            question.setOptions(optionsObj.toString());
        } else {
            question.setOptions("[]");
        }

        // 知识点
        Object tagsObj = body.get("categoryTags");
        if (tagsObj instanceof List) {
            question.setCategoryTags(JSON.toJSONString(tagsObj));
        } else if (tagsObj != null && !tagsObj.toString().isEmpty()) {
            question.setCategoryTags(tagsObj.toString());
        } else {
            question.setCategoryTags("[]");
        }

        // 难度
        Object diffObj = body.get("difficulty");
        question.setDifficulty(diffObj != null ? Integer.parseInt(diffObj.toString()) : 3);

        // 分值
        Object scoreObj = body.get("score");
        question.setScore(scoreObj != null ? new BigDecimal(scoreObj.toString()) : new BigDecimal("2.0"));

        question.setStatus(1);
        question.setUsageCount(0);

        return questionService.add(question, creatorId);
    }

    private String safeString(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    /**
     * 逻辑删除题目
     *
     * @param id 题目ID
     * @return Result
     */
    @Log(module = "题库管理", action = "删除题目")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return questionService.delete(id);
    }

    /**
     * 更新题目
     * PUT /api/question/update
     */
    @Log(module = "题库管理", action = "更新题目")
    @PutMapping("/update")
    public Result<?> update(@RequestBody Question question, HttpServletRequest request) {
        Long creatorId = getCurrentUserId(request);

        // 检查题目是否存在
        Question existing = questionService.getById(question.getId());
        if (existing == null) {
            return Result.error("题目不存在");
        }

        // 确保 JSON 字段为合法 JSON 字符串
        if (question.getAnswer() != null && !question.getAnswer().trim().isEmpty()) {
            // 如果 answer 是简单字符串（如 "C"），包装为 JSON 数组格式
            String answer = question.getAnswer().trim();
            if (!answer.startsWith("[") && !answer.startsWith("{")) {
                question.setAnswer("[\"" + answer.replace("\"", "\\\"") + "\"]");
            }
        }
        if (question.getOptions() != null && !question.getOptions().trim().isEmpty()) {
            String options = question.getOptions().trim();
            if (!options.startsWith("[")) {
                question.setOptions("[\"" + options.replace(",", "\",\"").replace("\"", "\\\"") + "\"]");
            }
        }
        if (question.getCategoryTags() != null && !question.getCategoryTags().trim().isEmpty()) {
            String tags = question.getCategoryTags().trim();
            if (!tags.startsWith("[")) {
                question.setCategoryTags("[\"" + tags.replace(",", "\",\"").replace("\"", "\\\"") + "\"]");
            }
        }

        // 更新题目
        boolean updated = questionService.updateById(question);
        if (updated) {
            return Result.success("更新成功", null);
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 从请求头获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                return JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {
                // ignore
            }
        }
        // 测试阶段默认返回 teacher01 (id=2)
        return 2L;
    }

    @Log(module = "题库管理", action = "批量导入题目")
    @PostMapping("/batch-import")
    public Result<?> batchImport(@RequestBody List<Question> questions, HttpServletRequest request) {
        Long creatorId = getCurrentUserId(request);
        int count = 0;
        int skip = 0;
        for (Question q : questions) {
            // 校验必填字段
            if (q.getContent() == null || q.getContent().trim().isEmpty()) {
                skip++;
                continue;
            }

            q.setCreatorId(creatorId);
            if (q.getScore() == null) q.setScore(new BigDecimal("2.0"));
            if (q.getDifficulty() == null) q.setDifficulty(3);
            if (q.getStatus() == null) q.setStatus(1);

            // answer：空则给默认值
            String answer = q.getAnswer();
            if (answer == null || answer.trim().isEmpty()) {
                q.setAnswer("[\"\"]");
            } else if (!answer.startsWith("[")) {
                q.setAnswer("[\"" + answer.replace("\"", "\\\"") + "\"]");
            }

            // options：空则给空数组
            String options = q.getOptions();
            if (options == null || options.trim().isEmpty()) {
                q.setOptions("[]");
            } else if (!options.startsWith("[")) {
                q.setOptions("[\"" + options.replace(",", "\",\"").replace("\"", "\\\"") + "\"]");
            }

            // categoryTags：空则给空数组
            String tags = q.getCategoryTags();
            if (tags == null || tags.trim().isEmpty()) {
                q.setCategoryTags("[]");
            }

            questionService.save(q);
            count++;
        }
        String msg = "成功导入 " + count + " 道题目";
        if (skip > 0) msg += "，跳过 " + skip + " 条（题目内容为空）";
        return Result.success(msg);
    }
}
