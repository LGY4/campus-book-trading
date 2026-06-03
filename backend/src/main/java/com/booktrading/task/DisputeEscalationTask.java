package com.booktrading.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booktrading.entity.Order;
import com.booktrading.entity.User;
import com.booktrading.service.MessageService;
import com.booktrading.service.OrderService;
import com.booktrading.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DisputeEscalationTask {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    // 每天早上9点检查超过3天未处理的纠纷
    @Scheduled(cron = "0 0 9 * * ?")
    public void escalateStaleDisputes() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "DISPUTE")
                .lt(Order::getUpdateTime, threshold);
        List<Order> staleDisputes = orderService.list(wrapper);

        if (staleDisputes.isEmpty()) return;

        List<User> admins = userService.list(
                new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));

        for (Order order : staleDisputes) {
            long daysOpen = java.time.temporal.ChronoUnit.DAYS.between(
                    order.getUpdateTime().toLocalDate(), LocalDateTime.now().toLocalDate());
            for (User admin : admins) {
                messageService.sendSystemMessage(admin.getId(),
                        "【催办】订单 " + order.getOrderNo() + " 纠纷已" + daysOpen + "天未处理，请尽快处理");
            }
        }
        log.info("Dispute escalation: notified {} admins about {} stale disputes", admins.size(), staleDisputes.size());
    }
}
