package com.booktrading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long bookId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal price;

    private String status;

    private String address;

    private String phone;

    private String receiverName;

    private String shippingInfo;

    private String logisticsCompany;

    private String trackingNumber;

    private String returnLogisticsCompany;

    private String returnTrackingNumber;

    private String refundReason;

    private String disputeReason;

    private String disputeImages;

    private String adminNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
