package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.Order;
import com.booktrading.entity.Payment;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.PaymentMapper;
import com.booktrading.service.OrderService;
import com.booktrading.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    @Autowired
    private OrderService orderService;

    @Override
    public Payment createPayment(Long orderId, BigDecimal amount) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getPrice() != null && amount.compareTo(order.getPrice()) != 0) {
            throw new BusinessException("支付金额与订单金额不一致");
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod("ALIPAY");
        payment.setStatus("PENDING");
        payment.setTransactionNo(generateTransactionNo());
        save(payment);
        return payment;
    }

    @Override
    @Transactional
    public boolean handleAlipayNotify(Map<String, String> params) {
        String tradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");

        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            return false;
        }

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getTransactionNo, tradeNo);
        Payment payment = getOne(wrapper);

        if (payment == null || !"PENDING".equals(payment.getStatus())) {
            return false;
        }

        payment.setStatus("SUCCESS");
        payment.setAlipayTradeNo(params.get("trade_no"));
        updateById(payment);

        orderService.payOrder(payment.getOrderId(), null);
        return true;
    }

    @Override
    @Transactional
    public boolean refund(Long orderId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, "SUCCESS");
        Payment payment = getOne(wrapper);

        if (payment == null) {
            throw new BusinessException("未找到支付记录");
        }

        payment.setStatus("REFUNDED");
        updateById(payment);
        return true;
    }

    @Override
    @Transactional
    public boolean partialRefund(Long orderId, BigDecimal amount) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, "SUCCESS");
        Payment payment = getOne(wrapper);

        if (payment == null) {
            throw new BusinessException("未找到支付记录");
        }
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("退款金额不能超过支付金额");
        }

        // 记录部分退款
        Payment refundRecord = new Payment();
        refundRecord.setOrderId(orderId);
        refundRecord.setAmount(amount.negate());
        refundRecord.setMethod("ALIPAY");
        refundRecord.setStatus("REFUNDED");
        refundRecord.setTransactionNo("RFD" + generateTransactionNo().substring(3));
        save(refundRecord);
        return true;
    }

    private String generateTransactionNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PAY" + timestamp + uuid;
    }
}
