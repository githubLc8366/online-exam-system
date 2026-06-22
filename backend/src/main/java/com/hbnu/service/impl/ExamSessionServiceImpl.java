package com.hbnu.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.BusinessException;
import com.hbnu.common.Result;
import com.hbnu.mapper.*;
import com.hbnu.pojo.*;
import com.hbnu.service.ExamSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试场次 Service 实现类
 */
@Service
public class ExamSessionServiceImpl implements ExamSessionService {

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private ExamSessionTargetMapper examSessionTargetMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    @Override
    @Transactional
    public Result<?> publishSession(Long sessionId, List<Long> targetIds) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error("考试场次不存在");
        }

        Paper paper = paperMapper.selectById(session.getPaperId());
        if (paper == null) {
            return Result.error("关联试卷不存在");
        }

        // 更新发布状态
        session.setPublishStatus(1);
        session.setParticipantCount(targetIds.size());
        examSessionMapper.updateById(session);

        // 为每个学生生成待考记录
        for (Long userId : targetIds) {
            // 查询是否已存在（排除已删除的记录）
            QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("session_id", sessionId)
                    .eq("user_id", userId)
                    .eq("is_deleted", 0);
            if (examRecordMapper.selectCount(wrapper) > 0) {
                continue;
            }

            // 查询用户信息以填充冗余字段
            User student = userMapper.selectById(userId);
            ExamRecord record = new ExamRecord();
            record.setSessionId(sessionId);
            record.setUserId(userId);
            record.setUserNo(student != null ? student.getUserNo() : String.valueOf(userId));
            record.setUserName(student != null ? student.getNickname() : "");
            record.setPaperId(session.getPaperId());
            record.setPaperName(paper.getPaperName());
            record.setStatus(0); // 待考
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            examRecordMapper.insert(record);
        }

        return Result.success("发布成功", null);
    }

    @Override
    @Transactional
    public Result<?> createAndPublish(Map<String, Object> params) {
        // 1. 解析开始时间，并校验不早于当前时间
        LocalDateTime startTime = LocalDateTime.parse(((String) params.get("startTime")).replace(" ", "T"));
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new BusinessException("开始时间不能早于当前时间");
        }

        // 2. 加载试卷：结束时间忽略前端传值，统一按 开始时间 + 试卷时长 计算
        Long paperId = Long.valueOf(params.get("paperId").toString());
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException("关联试卷不存在");
        }
        int duration = paper.getDuration() != null ? paper.getDuration() : 120;
        LocalDateTime endTime = startTime.plusMinutes(duration);

        // 3. 创建考试场次
        ExamSession session = new ExamSession();
        session.setSessionName((String) params.get("sessionName"));
        session.setPaperId(paperId);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setPublishStatus(1);
        session.setAllowLate(1);
        Object lateLimitObj = params.get("lateLimit");
        session.setLateLimit(lateLimitObj != null ? Integer.parseInt(lateLimitObj.toString()) : 0);
        session.setCheatMonitor(1);
        session.setMaxSwitch(3);
        session.setCreatorId(1L);
        session.setStatus(1);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        examSessionMapper.insert(session);

        // 3. 确定需要参加考试的学生列表
        List<Long> studentIds = new ArrayList<>();
        List<Long> classIds = new ArrayList<>();

        // 处理目标班级ID
        Object rawIds = params.get("targetIds");
        if (rawIds instanceof List) {
            for (Object id : (List<?>) rawIds) {
                classIds.add(Long.valueOf(id.toString()));
            }
        }
        // 快速交卷阈值
        Object thresholdObj = params.get("fastSubmitThreshold");
        session.setFastSubmitThreshold(thresholdObj != null ? Integer.parseInt(thresholdObj.toString()) : 20);
        // 乱序设置
        Object sqObj = params.get("shuffleQuestions");
        session.setShuffleQuestions(sqObj != null && Boolean.parseBoolean(sqObj.toString()) ? 1 : 0);
        Object soObj = params.get("shuffleOptions");
        session.setShuffleOptions(soObj != null && Boolean.parseBoolean(soObj.toString()) ? 1 : 0);

        // 检查是否有指定个人学生
        Object specificIds = params.get("specificStudentIds");
        if (specificIds instanceof List && !((List<?>) specificIds).isEmpty()) {
            for (Object id : (List<?>) specificIds) {
                studentIds.add(Long.valueOf(id.toString()));
            }
        } else {
            // 查询班级中的所有学生（通过角色关联表过滤，排除已删除）
            for (Long classId : classIds) {
                QueryWrapper<User> studentWrapper = new QueryWrapper<>();
                studentWrapper.eq("class_id", classId)
                        .eq("is_deleted", 0)
                        .eq("status", 1)
                        .inSql("id", "SELECT user_id FROM sys_user_role WHERE role_id = 3");
                List<User> students = userMapper.selectList(studentWrapper);
                for (User student : students) {
                    if (!studentIds.contains(student.getId())) {
                        studentIds.add(student.getId());
                    }
                }
            }
        }

        session.setParticipantCount(studentIds.size());
        examSessionMapper.updateById(session);

        // 4. 保存参与对象关联
        for (Long classId : classIds) {
            ExamSessionTarget target = new ExamSessionTarget();
            target.setSessionId(session.getId());
            target.setTargetType(1);
            target.setTargetId(classId);
            target.setCreateTime(LocalDateTime.now());
            target.setUpdateTime(LocalDateTime.now());
            examSessionTargetMapper.insert(target);
        }

        // 5. 为每个学生生成考试记录（paper 已在上方加载）
        for (Long userId : studentIds) {
            // 检查是否已存在（排除已删除）
            QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("session_id", session.getId())
                    .eq("user_id", userId)
                    .eq("is_deleted", 0);
            if (examRecordMapper.selectCount(wrapper) > 0) {
                continue;
            }

            User student = userMapper.selectById(userId);
            ExamRecord record = new ExamRecord();
            record.setSessionId(session.getId());
            record.setUserId(userId);
            record.setUserNo(student != null ? student.getUserNo() : String.valueOf(userId));
            record.setUserName(student != null ? student.getNickname() : "");
            record.setPaperId(session.getPaperId());
            record.setPaperName(paper.getPaperName());
            record.setStatus(0);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            examRecordMapper.insert(record);
        }

        return Result.success("发布成功", session.getId());
    }

    @Override
    public Result<?> getDetail(Long id) {
        ExamSession session = examSessionMapper.selectById(id);
        if (session == null || session.getIsDeleted() == 1) {
            return Result.error("考试场次不存在");
        }

        // 【新增】查询试卷信息
        Map<String, Object> data = new HashMap<>();
        data.put("id", session.getId());
        data.put("sessionName", session.getSessionName());
        data.put("paperId", session.getPaperId());
        data.put("startTime", session.getStartTime());
        data.put("endTime", session.getEndTime());
        data.put("publishStatus", session.getPublishStatus());
        data.put("participantCount", session.getParticipantCount());
        data.put("cheatMonitor", session.getCheatMonitor());
        data.put("maxSwitch", session.getMaxSwitch());

        // 查询试卷获取名称和时长
        Paper paper = paperMapper.selectById(session.getPaperId());
        if (paper != null) {
            data.put("paperName", paper.getPaperName());
            data.put("duration", paper.getDuration());
        }

        return Result.success(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(Long id) {
        ExamSession session = examSessionMapper.selectById(id);
        if (session == null || session.getIsDeleted() == 1) {
            return Result.error("考试场次不存在");
        }

        // 逻辑删除该场次下的所有考试记录（update 走 UpdateWrapper 显式 set is_deleted=1，绕开 @TableLogic 剔除）
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ExamRecord> recordUpdate =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        recordUpdate.eq("session_id", id).eq("is_deleted", 0).set("is_deleted", 1);
        examRecordMapper.update(null, recordUpdate);

        // 逻辑删除考试场次本身：用 deleteById 让 MP 走 @TableLogic 路径
        int affected = examSessionMapper.deleteById(id);
        if (affected == 0) {
            return Result.error("删除失败，记录不存在或已被删除");
        }
        return Result.success("删除成功");
    }

    @Override
    public Result<IPage<ExamSession>> pageList(Integer page, Integer size, String sessionName, Integer publishStatus) {
        Page<ExamSession> pageParam = new Page<>(page, size);
        QueryWrapper<ExamSession> queryWrapper = new QueryWrapper<>();

        // 【修复】过滤已删除的记录
        queryWrapper.eq("is_deleted", 0);

        if (sessionName != null && !sessionName.isEmpty()) {
            queryWrapper.like("session_name", sessionName);
        }
        if (publishStatus != null) {
            queryWrapper.eq("publish_status", publishStatus);
        }

        queryWrapper.orderByDesc("create_time");

        IPage<ExamSession> result = examSessionMapper.selectPage(pageParam, queryWrapper);
        return Result.success(result);
    }

    @Override
    public Result<List<Map<String, Object>>> getTargets(Long sessionId) {
        QueryWrapper<ExamSessionTarget> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .eq("is_deleted", 0);
        List<ExamSessionTarget> targets = examSessionTargetMapper.selectList(wrapper);

        // 批量查询班级名 / 学生姓名，避免 N+1
        List<Long> classIds = targets.stream()
                .filter(t -> Integer.valueOf(1).equals(t.getTargetType()))
                .map(ExamSessionTarget::getTargetId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> studentIds = targets.stream()
                .filter(t -> Integer.valueOf(2).equals(t.getTargetType()))
                .map(ExamSessionTarget::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> classNameMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            List<Clazz> classes = clazzMapper.selectBatchIds(classIds);
            for (Clazz c : classes) {
                classNameMap.put(c.getId(), c.getClassName());
            }
        }
        Map<Long, String> studentNameMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<User> students = userMapper.selectBatchIds(studentIds);
            for (User u : students) {
                studentNameMap.put(u.getId(), u.getNickname());
            }
        }

        List<Map<String, Object>> result = targets.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("targetType", t.getTargetType());
            item.put("targetId", t.getTargetId());
            if (Integer.valueOf(1).equals(t.getTargetType())) {
                item.put("targetName", classNameMap.getOrDefault(t.getTargetId(), "未知班级"));
            } else if (Integer.valueOf(2).equals(t.getTargetType())) {
                item.put("targetName", studentNameMap.getOrDefault(t.getTargetId(), "未知学生"));
            } else {
                item.put("targetName", "");
            }
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<List<Map<String, Object>>> getMonitorData(Long sessionId) {
        // 1. 获取该场次的所有考试记录（排除已删除）
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .eq("is_deleted", 0);
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        // 2. 获取场次信息
        ExamSession session = examSessionMapper.selectById(sessionId);

        // 3. 批量查询用户信息
        List<Long> userIds = records.stream()
                .map(ExamRecord::getUserId)
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        Map<Long, String> classNameMap = new HashMap<>();

        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }

            List<Long> classIds = users.stream()
                    .map(User::getClassId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!classIds.isEmpty()) {
                List<Clazz> classes = clazzMapper.selectBatchIds(classIds);
                for (Clazz c : classes) {
                    classNameMap.put(c.getId(), c.getClassName());
                }
            }
        }

        // 4. 组装监控数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord record : records) {
            Map<String, Object> student = new HashMap<>();

            // 【关键修复】添加 recordId 字段
            student.put("recordId", record.getId());
            student.put("studentId", record.getUserId());
            student.put("studentNo", record.getUserNo());
            student.put("name", record.getUserName());

            User user = userMap.get(record.getUserId());
            if (user != null && user.getClassId() != null) {
                student.put("className", classNameMap.getOrDefault(user.getClassId(), "未知班级"));
            } else {
                student.put("className", "未设置");
            }

            // 状态转换
            String status;
            if (record.getStatus() == 0) {
                status = "not_started";
            } else if (record.getStatus() == 1) {
                status = "ongoing";
            } else if (record.getStatus() >= 2) {
                status = "submitted";
            } else {
                status = "not_started";
            }
            student.put("status", status);

            // 剩余时间（秒）
            long remainingTime = 0;
            if (record.getStatus() == 1 && session != null && session.getEndTime() != null) {
                remainingTime = java.time.Duration.between(
                        LocalDateTime.now(), session.getEndTime()).getSeconds();
                if (remainingTime < 0) remainingTime = 0;
            }
            student.put("remainingTime", remainingTime);

            student.put("switchCount", record.getSwitchCount() != null ? record.getSwitchCount() : 0);
            student.put("ip", record.getIpAddress() != null ? record.getIpAddress() : "-");
            boolean isAbnormal = (record.getSwitchCount() != null && record.getSwitchCount() > 0)
                    || (record.getCheatLogs() != null && !record.getCheatLogs().isEmpty());
            student.put("abnormal", isAbnormal);
            student.put("hasFastSubmit", record.getCheatLogs() != null && !record.getCheatLogs().isEmpty());

            // 【新增】答题统计 - 解析已答题数
            int answeredCount = 0;
            int totalCount = 0;
            if (record.getFinalAnswers() != null && !record.getFinalAnswers().isEmpty()) {
                try {
                    Map<String, Object> answers = JSON.parseObject(record.getFinalAnswers());
                    answeredCount = answers.size();
                } catch (Exception e) {}
            }
            student.put("answeredCount", answeredCount);
            student.put("totalCount", totalCount);

            result.add(student);
        }

        return Result.success(result);
    }
    @Override
    @Transactional
    public Result<?> extendTime(Long recordId, Integer extraMinutes) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || record.getIsDeleted() == 1) {
            return Result.error("考试记录不存在");
        }

        if (record.getStatus() != 1) {
            return Result.error("该考生不在考试中");
        }

        // 获取考试场次
        ExamSession session = examSessionMapper.selectById(record.getSessionId());
        if (session == null) {
            return Result.error("考试场次不存在");
        }

        // 延长结束时间
        LocalDateTime newEndTime = session.getEndTime().plusMinutes(extraMinutes);
        session.setEndTime(newEndTime);
        examSessionMapper.updateById(session);

        // 【新增】计算新的剩余时间返回给前端
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), newEndTime).getSeconds();
        if (remainingSeconds < 0) remainingSeconds = 0;

        Map<String, Object> data = new HashMap<>();
        data.put("remainingTime", remainingSeconds);
        data.put("message", "已延长 " + extraMinutes + " 分钟");

        return Result.success("已延长 " + extraMinutes + " 分钟", data);
    }
    @Override
    @Transactional
    public Result<?> forceSubmit(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || record.getIsDeleted() == 1) {
            return Result.error("考试记录不存在");
        }

        if (record.getStatus() != 1) {
            return Result.error("该考生不在考试中");
        }

        // 标记为异常强制交卷
        record.setStatus(2);
        record.setSubmitType(3);
        record.setSubmitTime(LocalDateTime.now());
        examRecordMapper.updateById(record);

        Map<String, Object> data = new HashMap<>();
        data.put("status", record.getStatus());
        data.put("submitType", 3);
        data.put("message", "已强制交卷");

        return Result.success("已强制交卷", data);
    }

}