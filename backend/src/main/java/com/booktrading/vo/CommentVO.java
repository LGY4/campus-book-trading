package com.booktrading.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long orderId;
    private Long parentId;
    private Long bookId;
    private String bookTitle;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private Integer rating;
    private String imagesJson;
    private LocalDateTime createTime;
    private List<CommentVO> followUps;
}
