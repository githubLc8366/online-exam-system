package com.hbnu.aop;

import com.hbnu.annotation.Log;
import com.hbnu.common.JwtUtils;
import com.hbnu.mapper.LogMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.pojo.LogEntry;
import com.hbnu.pojo.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private UserMapper userMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        Object result = null;
        boolean success = true;
        String errorMsg = null;

        try {
            result = point.proceed();
        } catch (Exception e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    LogEntry entry = new LogEntry();
                    entry.setModule(logAnnotation.module());
                    entry.setAction(logAnnotation.action());
                    entry.setContent(logAnnotation.module() + " - " + logAnnotation.action());
                    entry.setRequestMethod(request.getMethod());
                    entry.setRequestUrl(request.getRequestURI());
                    entry.setIpAddress(request.getRemoteAddr());
                    entry.setStatus(success ? 1 : 0);
                    entry.setErrorMsg(errorMsg);
                    entry.setCreateTime(LocalDateTime.now());

                    // 从 Token 获取用户信息
                    String token = request.getHeader("Authorization");
                    if (token != null && !token.isEmpty()) {
                        try {
                            token = token.replace("Bearer ", "");
                            Long userId = JwtUtils.getUserId(token);
                            String roleCode = JwtUtils.getRole(token);
                            entry.setUserId(userId);
                            entry.setRoleCode(roleCode);

                            // 查询用户工号和姓名
                            User user = userMapper.selectById(userId);
                            if (user != null) {
                                entry.setUserNo(user.getUserNo());
                                entry.setUserName(user.getNickname());
                            }
                        } catch (Exception e) {
                            // Token 解析失败，设为未知
                        }
                    }

                    // 兜底：如果还是没有 user_no，设置默认值
                    if (entry.getUserNo() == null || entry.getUserNo().isEmpty()) {
                        entry.setUserNo("unknown");
                        entry.setUserName("未知用户");
                    }

                    logMapper.insert(entry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}