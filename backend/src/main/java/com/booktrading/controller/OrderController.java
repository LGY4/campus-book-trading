package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.OrderCreateDTO;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.exception.BusinessException;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.OrderService;
import com.booktrading.vo.OrderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/create")
    @LogOperation(module = "订单", action = "创建")
    public Result<?> create(@RequestBody OrderCreateDTO dto) {
        if (dto.getBookId() == null) {
            throw new BusinessException("缺少bookId参数");
        }
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            throw new BusinessException("请填写收货地址");
        }
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            throw new BusinessException("请填写手机号");
        }
        if (dto.getReceiverName() == null || dto.getReceiverName().trim().isEmpty()) {
            throw new BusinessException("请填写收货人");
        }
        Long buyerId = currentUser.getId();
        com.booktrading.entity.Order order = orderService.createOrder(dto, buyerId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        return Result.ok(data);
    }

    @PostMapping("/pay/{orderId}")
    @LogOperation(module = "订单", action = "支付")
    public Result<?> pay(@PathVariable Long orderId) {
        orderService.payOrder(orderId, currentUser.getId());
        return Result.ok("支付成功");
    }

    @PostMapping("/ship")
    @LogOperation(module = "订单", action = "发货")
    public Result<?> ship(@RequestBody Map<String, Object> body) {
        if (body.get("orderId") == null) throw new BusinessException("缺少orderId参数");
        Long orderId = parseOrderId(body.get("orderId"));
        String shippingInfo = (String) body.get("shippingInfo");
        String logisticsCompany = (String) body.get("logisticsCompany");
        String trackingNumber = (String) body.get("trackingNumber");
        orderService.shipOrder(orderId, currentUser.getId(), shippingInfo, logisticsCompany, trackingNumber);
        return Result.ok("发货成功");
    }

    @PostMapping("/confirm/{orderId}")
    @LogOperation(module = "订单", action = "确认收货")
    public Result<?> confirmReceive(@PathVariable Long orderId) {
        orderService.confirmReceive(orderId, currentUser.getId());
        return Result.ok("确认收货成功");
    }

    @PostMapping("/cancel/{orderId}")
    @LogOperation(module = "订单", action = "取消")
    public Result<?> cancel(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId, currentUser.getId());
        return Result.ok("取消成功");
    }

    @PostMapping("/refund")
    @LogOperation(module = "订单", action = "申请退款")
    public Result<?> refund(@RequestBody Map<String, Object> body) {
        if (body.get("orderId") == null) throw new BusinessException("缺少orderId参数");
        Long orderId = parseOrderId(body.get("orderId"));
        String reason = (String) body.get("reason");
        orderService.requestRefund(orderId, currentUser.getId(), reason);
        return Result.ok("退款申请已提交");
    }

    @PostMapping("/confirm-refund/{orderId}")
    @LogOperation(module = "订单", action = "确认退款")
    public Result<?> confirmRefund(@PathVariable Long orderId) {
        orderService.confirmRefund(orderId, currentUser.getId());
        return Result.ok("退款已确认");
    }

    @PostMapping("/reject-refund/{orderId}")
    @LogOperation(module = "订单", action = "拒绝退款")
    public Result<?> rejectRefund(@PathVariable Long orderId) {
        orderService.rejectRefund(orderId, currentUser.getId());
        return Result.ok("已拒绝退款");
    }

    @PostMapping("/return-shipping")
    @LogOperation(module = "订单", action = "提交退回物流")
    public Result<?> returnShipping(@RequestBody Map<String, Object> body) {
        if (body.get("orderId") == null) throw new BusinessException("缺少orderId参数");
        Long orderId = parseOrderId(body.get("orderId"));
        String logisticsCompany = (String) body.get("logisticsCompany");
        String trackingNumber = (String) body.get("trackingNumber");
        if (logisticsCompany == null || logisticsCompany.trim().isEmpty()) {
            throw new BusinessException("请选择物流公司");
        }
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new BusinessException("请输入物流单号");
        }
        orderService.submitReturnShipping(orderId, currentUser.getId(), logisticsCompany, trackingNumber);
        return Result.ok("退回物流信息已提交");
    }

    @PostMapping("/confirm-return/{orderId}")
    @LogOperation(module = "订单", action = "确认退回")
    public Result<?> confirmReturn(@PathVariable Long orderId) {
        orderService.confirmReturnReceive(orderId, currentUser.getId());
        return Result.ok("已确认收到退回书籍，退款已完成");
    }

    @PostMapping("/dispute")
    @LogOperation(module = "订单", action = "提交纠纷")
    public Result<?> dispute(@RequestBody Map<String, Object> body) {
        if (body.get("orderId") == null) throw new BusinessException("缺少orderId参数");
        if (body.get("reason") == null || ((String) body.get("reason")).trim().isEmpty()) {
            throw new BusinessException("请填写纠纷原因");
        }
        Long orderId = parseOrderId(body.get("orderId"));
        String reason = (String) body.get("reason");
        String disputeImages = (String) body.get("disputeImages");
        orderService.disputeOrder(orderId, currentUser.getId(), reason, disputeImages);
        return Result.ok("争议已提交");
    }

    @GetMapping("/my-buy")
    public Result<?> myBuyOrders(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String status) {
        IPage<?> result = orderService.getMyBuyOrders(currentUser.getId(), page, size, status);
        return Result.ok(PageResult.from(result));
    }

    @GetMapping("/my-sell")
    public Result<?> mySellOrders(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String status) {
        IPage<?> result = orderService.getMySellOrders(currentUser.getId(), page, size, status);
        return Result.ok(PageResult.from(result));
    }

    @GetMapping("/completed-by-book/{bookId}")
    public Result<?> completedByBook(@PathVariable Long bookId) {
        return Result.ok(orderService.getCompletedOrdersByBook(bookId, currentUser.getId()));
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        OrderVO vo = orderService.getOrderDetail(id);
        if (vo == null) {
            return Result.error("订单不存在");
        }
        Long uid = currentUser.getId();
        if (uid != null && !uid.equals(vo.getBuyerId()) && !uid.equals(vo.getSellerId()) && !currentUser.isAdmin()) {
            return Result.error("无权查看此订单");
        }
        return Result.ok(vo);
    }

    private Long parseOrderId(Object obj) {
        try {
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("orderId格式不正确");
        }
    }
}
