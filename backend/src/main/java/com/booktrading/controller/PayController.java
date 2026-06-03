package com.booktrading.controller;

import com.booktrading.dto.Result;
import com.booktrading.entity.Order;
import com.booktrading.entity.Payment;
import com.booktrading.exception.BusinessException;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.OrderService;
import com.booktrading.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器 - 沙箱Mock实现
 * 直接模拟支付成功，完成订单状态流转
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CurrentUser currentUser;

    /**
     * 创建支付 - Mock模式：创建支付记录后直接完成支付
     */
    @PostMapping("/create/{orderId}")
    public Result<?> createPayment(@PathVariable Long orderId) {
        Long userId = currentUser.getId();
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 创建支付记录
        Payment payment = paymentService.createPayment(orderId, order.getPrice());
        // Mock模式直接标记支付成功
        payment.setStatus("SUCCESS");
        paymentService.updateById(payment);

        // 更新订单状态
        orderService.payOrder(orderId, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("transactionNo", payment.getTransactionNo());
        data.put("status", "SUCCESS");
        data.put("message", "支付成功（沙箱模拟）");
        return Result.ok(data);
    }

    /**
     * 支付宝同步回调（保留接口，实际由 create 直接完成）
     */
    @GetMapping("/return")
    public Result<?> payReturn(@RequestParam Long orderId) {
        return Result.ok(orderId);
    }
}
