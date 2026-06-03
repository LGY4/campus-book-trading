package com.booktrading.aspect;

import com.booktrading.entity.OperationLog;
import com.booktrading.entity.User;
import com.booktrading.mapper.OperationLogMapper;
import com.booktrading.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Pointcut("@annotation(com.booktrading.aspect.LogOperation)")
    public void logPointcut() {}

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            long costTime = System.currentTimeMillis() - startTime;
            try {
                saveLog(joinPoint, costTime, ex.getMessage());
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
            throw ex;
        }
        long costTime = System.currentTimeMillis() - startTime;
        try {
            saveLog(joinPoint, costTime, null);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, long costTime, String errorMsg) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogOperation logAnnotation = method.getAnnotation(LogOperation.class);

        OperationLog operationLog = new OperationLog();
        operationLog.setModule(logAnnotation.module());
        operationLog.setAction(logAnnotation.action());

        String className = signature.getDeclaringType().getSimpleName();
        operationLog.setMethod(className + "." + method.getName());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String requestPath = "";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestPath = request.getMethod() + " " + request.getRequestURI();
            operationLog.setIp(request.getRemoteAddr());
            Long userId = (Long) request.getAttribute("currentUserId");
            if (userId != null) {
                operationLog.setUserId(userId);
                try {
                    User user = userService.getById(userId);
                    if (user != null) {
                        operationLog.setUsername(user.getUsername());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 生成中文描述
        StringBuilder desc = new StringBuilder();
        desc.append("执行了【").append(logAnnotation.action()).append("】操作");
        if (!requestPath.isEmpty()) {
            desc.append("，请求: ").append(requestPath);
        }
        if (costTime > 0) {
            desc.append("，耗时: ").append(costTime).append("ms");
        }
        if (errorMsg != null) {
            desc.append("，失败原因: ").append(errorMsg);
        }
        operationLog.setDetail(desc.toString());

        operationLog.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
    }
}
