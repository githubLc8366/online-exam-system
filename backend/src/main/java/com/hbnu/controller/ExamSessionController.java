package com.hbnu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hbnu.annotation.Log;
import com.hbnu.common.Result;
import com.hbnu.pojo.ExamSession;
import com.hbnu.service.ExamSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;



/**
 * 考试场次控制器
 */
@RestController
@RequestMapping("/api/session")
public class ExamSessionController {

    @Autowired
    private ExamSessionService examSessionService;
    /**
     * 延长考生考试时间
     * PUT /api/session/extend/{recordId}
     */
    @Log(module = "考试管理", action = "延长考试时间")
    @PutMapping("/extend/{recordId}")
    public Result<?> extendTime(@PathVariable Long recordId, @RequestBody Map<String, Object> params) {
        Integer extraMinutes = params.get("extraMinutes") != null ?
                Integer.parseInt(params.get("extraMinutes").toString()) : 10;
        return examSessionService.extendTime(recordId, extraMinutes);
    }

    /**
     * 强制交卷
     * POST /api/session/force-submit/{recordId}
     */
    @Log(module = "考试管理", action = "强制交卷")
    @PostMapping("/force-submit/{recordId}")
    public Result<?> forceSubmit(@PathVariable Long recordId) {
        return examSessionService.forceSubmit(recordId);
    }

    /**
     * 分页查询考试场次列表
     *
     * @param page          当前页
     * @param size          每页大小
     * @param sessionName   考试名称（模糊查询）
     * @param publishStatus 发布状态
     * @return Result
     */
    @GetMapping("/list")
    public Result<IPage<ExamSession>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sessionName,
            @RequestParam(required = false) Integer publishStatus) {
        return examSessionService.pageList(page, size, sessionName, publishStatus);
    }

    /**
     * 发布考试场次 - 创建并发布
     *
     * @param params 包含 sessionName, paperId, startTime, endTime, targetType,
     *               targetIds
     * @return Result
     */
    @Log(module = "考试管理", action = "发布考试")
    @PostMapping("/publish")
    public Result<?> publish(@RequestBody Map<String, Object> params) {
        return examSessionService.createAndPublish(params);
    }

    /**
     * 获取考试场次详情
     */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return examSessionService.getDetail(id);
    }

    /**
     * 删除考试场次
     */
    @Log(module = "考试管理", action = "取消考试")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return examSessionService.delete(id);
    }

    /**
     * 获取考试场次的参与对象列表
     */
    /**
     * 获取考试场次的参与对象列表
     */
    @GetMapping("/targets/{sessionId}")
    public Result<List<Map<String, Object>>> getTargets(@PathVariable Long sessionId) {
        return examSessionService.getTargets(sessionId);
    }
    /**
     * 获取考试监控数据（考生实时状态）
     */
    @GetMapping("/monitor/{sessionId}")
    public Result<List<Map<String, Object>>> getMonitorData(@PathVariable Long sessionId) {
        return examSessionService.getMonitorData(sessionId);
    }

}
