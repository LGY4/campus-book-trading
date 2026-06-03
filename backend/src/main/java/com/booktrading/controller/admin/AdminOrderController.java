package com.booktrading.controller.admin;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.Order;
import com.booktrading.service.OrderService;
import com.booktrading.vo.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = orderService.page(new Page<>(page, size), wrapper);
        IPage<OrderVO> voPage = orderPage.convert(order -> orderService.getOrderDetail(order.getId()));
        return Result.ok(PageResult.from(voPage));
    }

    @GetMapping("/disputes")
    public Result<?> disputes(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "DISPUTE");
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = orderService.page(new Page<>(page, size), wrapper);
        IPage<OrderVO> voPage = orderPage.convert(order -> orderService.getOrderDetail(order.getId()));
        return Result.ok(PageResult.from(voPage));
    }

    @PostMapping("/resolve-dispute")
    @LogOperation(module = "订单管理", action = "处理纠纷")
    public Result<?> resolveDispute(@RequestBody Map<String, Object> body) {
        Object orderIdObj = body.get("orderId");
        if (orderIdObj == null) {
            return Result.error("缺少orderId参数");
        }
        Long orderId;
        try {
            orderId = Long.valueOf(orderIdObj.toString());
        } catch (NumberFormatException e) {
            return Result.error("orderId格式不正确");
        }
        String adminNote = (String) body.get("adminNote");
        String action = (String) body.get("action");
        if (action == null) {
            action = "refund";
        }
        java.math.BigDecimal partialRefundAmount = null;
        if (body.get("partialRefundAmount") != null) {
            partialRefundAmount = new java.math.BigDecimal(body.get("partialRefundAmount").toString());
        }
        orderService.resolveDispute(orderId, adminNote, action, partialRefundAmount);
        return Result.ok("争议已处理");
    }

    @GetMapping("/export")
    @LogOperation(module = "订单管理", action = "导出")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderService.list(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=orders.csv");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.println("ID,订单号,书籍ID,买家ID,卖家ID,金额,状态,收货人,手机号,地址,创建时间");
        for (Order o : orders) {
            writer.println(o.getId() + ","
                    + escape(o.getOrderNo()) + ","
                    + o.getBookId() + ","
                    + o.getBuyerId() + ","
                    + o.getSellerId() + ","
                    + o.getPrice() + ","
                    + o.getStatus() + ","
                    + escape(o.getReceiverName()) + ","
                    + escape(o.getPhone()) + ","
                    + escape(o.getAddress()) + ","
                    + o.getCreateTime());
        }
        writer.flush();
    }

    private String escape(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
