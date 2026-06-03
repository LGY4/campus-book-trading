package com.booktrading.dto;

import lombok.Data;

@Data
public class OrderCreateDTO {
    private Long bookId;
    private String address;
    private String phone;
    private String receiverName;
}
