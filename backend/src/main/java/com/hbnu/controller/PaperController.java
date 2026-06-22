package com.hbnu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hbnu.annotation.Log;
import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.dto.PaperCreateDTO;
import com.hbnu.pojo.Paper;
import com.hbnu.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 试卷管理控制器
 */
@RestController
@RequestMapping("/api/paper")
public class PaperController {

    @Autowired
    private PaperService paperService;


    /**
     * 分页查询试卷列表
     */
    @GetMapping("/list")
    public Result<IPage<Paper>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer status) {
        return paperService.pageList(page, size, subject, status);
    }

    /**
     * 创建试卷
     */
    @Log(module = "试卷管理", action = "创建试卷")
    @PostMapping("/create")
    public Result<?> create(@RequestBody PaperCreateDTO dto, HttpServletRequest request) {
        // 从 Token 中获取当前教师ID（简化处理，固定为1）
        Long creatorId = 1L;
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                creatorId = JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {
                // ignore
            }
        }
        return paperService.createPaper(dto, creatorId);
    }

    /**
     * 获取试卷详情
     */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return paperService.getDetail(id);
    }

    /**
     * 删除试卷
     */
    @Log(module = "试卷管理", action = "删除试卷")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return paperService.delete(id);
    }

    /**
     * 更新试卷
     * PUT /api/paper/update
     */
    @Log(module = "试卷管理", action = "编辑试卷")
    @PutMapping("/update")
    public Result<?> update(@RequestBody PaperCreateDTO dto, HttpServletRequest request) {
        Long creatorId = 1L;
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                creatorId = JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {
                // ignore
            }
        }
        return paperService.updatePaper(dto, creatorId);
    }

    /**
     * 获取各题型可用数量（用于随机组卷时参考）
     * GET /api/paper/question-stats?subject=Java程序设计
     */
    @GetMapping("/question-stats")
    public Result<Map<String, Object>> getQuestionStats(
            @RequestParam(required = false) String subject) {
        return paperService.getQuestionStats(subject);
    }
}
