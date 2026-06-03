package com.booktrading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String author;

    private String publisher;

    private String isbn;

    private BigDecimal originalPrice;

    private BigDecimal sellingPrice;

    @TableField("book_condition")
    private String bookCondition;

    private String description;

    private String coverImage;

    private String imagesJson;

    private Long categoryId;

    private Long sellerId;

    private Integer quantity;

    private String status;

    private String rejectReason;

    private Integer viewCount;

    private Integer wantCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
