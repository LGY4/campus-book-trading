package com.booktrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService extends IService<Payment> {

    Payment createPayment(Long orderId, BigDecimal amount);

    boolean handleAlipayNotify(Map<String, String> params);

    boolean refund(Long orderId);

    boolean partialRefund(Long orderId, BigDecimal amount);
}
