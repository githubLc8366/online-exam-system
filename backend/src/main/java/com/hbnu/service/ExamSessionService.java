package com.hbnu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hbnu.common.Result;
import com.hbnu.pojo.ExamSession;

import java.util.List;
import java.util.Map;

/**
 * 考试场次 Service 接口
 */
public interface ExamSessionService {
    Result<?> forceSubmit(Long recordId);

    Result<?> extendTime(Long recordId, Integer extraMinutes);

    /**
     * 发布考试场次
     *
     * @param sessionId 场次ID
     * @param targetIds 目标ID列表（班级ID或学生ID）
     * @return Result
     */
    Result<?> publishSession(Long sessionId, List<Long> targetIds);

    /**
     * 分页查询考试场次列表
     *
     * @param page          当前页
     * @param size          每页大小
     * @param sessionName   考试名称（模糊查询）
     * @param publishStatus 发布状态
     * @return Result
     */
    Result<IPage<ExamSession>> pageList(Integer page, Integer size, String sessionName, Integer publishStatus);

    /**
     * 创建并发布考试场次
     *
     * @param params 包含 sessionName, paperId, startTime, endTime, targetType,
     *               targetIds
     * @return Result
     */
    Result<?> createAndPublish(Map<String, Object> params);

    /**
     * 获取考试场次详情
     *
     * @param id 场次ID
     * @return Result
     */
    Result<?> getDetail(Long id);

    /**
     * 删除考试场次
     *
     * @param id 场次ID
     * @return Result
     */
    Result<?> delete(Long id);

    /**
     * 获取考试场次的参与对象列表
     */
    Result<List<Map<String, Object>>> getTargets(Long sessionId);

    /**
     * 获取考试监控数据（考生实时状态）
     */
    Result<List<Map<String, Object>>> getMonitorData(Long sessionId);
}
