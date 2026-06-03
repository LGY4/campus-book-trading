package com.booktrading.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExchangeVO {
    private Long id;
    private Long initiatorBookId;
    private String initiatorBookTitle;
    private String initiatorBookCover;
    private Long targetBookId;
    private String targetBookTitle;
    private String targetBookCover;
    private Long initiatorId;
    private String initiatorNickname;
    private String initiatorAvatar;
    private Long targetUserId;
    private String targetUserNickname;
    private String targetUserAvatar;
    private String status;
    private String message;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
