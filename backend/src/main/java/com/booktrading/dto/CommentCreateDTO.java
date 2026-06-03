package com.booktrading.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {
    private Long orderId;
    private Long parentId;
    private Long bookId;
    private String content;
    private Integer rating;
    private String images;
}
