package com.booktrading.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long bookId;
    private String bookTitle;
    private String coverImage;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
