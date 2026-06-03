package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    void log(Long userId, String username, String module, String action,
             String detail, String ip, String method);

    IPage<OperationLog> listLogs(int page, int size, String keyword, String module, String startDate, String endDate);
}
