package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.OperationLog;
import com.booktrading.mapper.OperationLogMapper;
import com.booktrading.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public void log(Long userId, String username, String module, String action,
                    String detail, String ip, String method) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(ip);
        log.setMethod(method);
        log.setCreateTime(LocalDateTime.now());
        save(log);
    }

    @Override
    public IPage<OperationLog> listLogs(int page, int size, String keyword, String module, String startDate, String endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getModule, keyword)
                    .or().like(OperationLog::getAction, keyword)
                    .or().like(OperationLog::getDetail, keyword));
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(OperationLog::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(OperationLog::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }
}
