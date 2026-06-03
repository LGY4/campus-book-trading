package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.dto.ExchangeCreateDTO;
import com.booktrading.entity.Exchange;
import com.booktrading.vo.ExchangeVO;

public interface ExchangeService extends IService<Exchange> {

    void createExchange(ExchangeCreateDTO dto, Long userId);

    void acceptExchange(Long id, Long userId);

    void rejectExchange(Long id, Long userId);

    void completeExchange(Long id, Long userId);

    IPage<ExchangeVO> getMyExchanges(Long userId, int page, int size, String type);

    void cancelExchange(Long id, Long userId);
}
