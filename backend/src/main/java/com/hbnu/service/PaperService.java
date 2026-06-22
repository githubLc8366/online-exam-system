package com.hbnu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hbnu.common.Result;
import com.hbnu.dto.PaperCreateDTO;
import com.hbnu.pojo.Paper;

import java.util.Map;

/**
 * 试卷 Service 接口
 */
public interface PaperService {

    /**
     * 创建试卷（包含题目关联）
     *
     * @param dto       试卷创建请求
     * @param creatorId 创建教师ID
     * @return Result
     */
    Result<?> createPaper(PaperCreateDTO dto, Long creatorId);

    /**
     * 获取试卷详情（包含题目列表）
     *
     * @param id 试卷ID
     * @return Result
     */
    Result<?> getDetail(Long id);

    /**
     * 分页查询试卷列表
     *
     * @param page    当前页
     * @param size    每页大小
     * @param subject 科目
     * @param status  状态
     * @return Result
     */
    Result<IPage<Paper>> pageList(Integer page, Integer size, String subject, Integer status);

    /**
     * 删除试卷（级联删除关联表数据）
     *
     * @param id 试卷ID
     * @return Result
     */
    Result<?> delete(Long id);

    /**
     * 更新试卷（先删旧关联，再插入新关联）
     *
     * @param dto       试卷更新请求
     * @param creatorId 操作教师ID
     * @return Result
     */
    Result<?> updatePaper(PaperCreateDTO dto, Long creatorId);
    /**
     * 获取各题型可用数量统计
     */
    Result<Map<String, Object>> getQuestionStats(String subject);
}
