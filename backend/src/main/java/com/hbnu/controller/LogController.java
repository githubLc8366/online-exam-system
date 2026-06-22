package com.hbnu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.Result;
import com.hbnu.mapper.LogMapper;
import com.hbnu.pojo.LogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class LogController {

    @Autowired
    private LogMapper logMapper;

    @GetMapping("/system-log/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Page<LogEntry> pageParam = new Page<>(page, pageSize);
        QueryWrapper<LogEntry> wrapper = new QueryWrapper<>();

        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like("user_name", operatorName);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq("module", module);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("create_time", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("create_time", endDate + " 23:59:59");
        }
        wrapper.orderByDesc("create_time");

        IPage<LogEntry> result = logMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 批量删除日志
     */
    @DeleteMapping("/system-log/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        logMapper.deleteBatchIds(ids);
        return Result.success("删除成功");
    }

    /**
     * 清空所有日志
     */
    @DeleteMapping("/system-log/clear")
    public Result<?> clearAll() {
        logMapper.delete(new QueryWrapper<>());
        return Result.success("清空成功");
    }
}