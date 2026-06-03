package com.booktrading.dto;

import lombok.Data;

@Data
public class BookQueryDTO {
    private String keyword;
    private Long categoryId;
    private Integer page = 1;
    private Integer pageSize = 10;
    private Long userId;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
}
