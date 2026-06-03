package com.booktrading.dto;

import lombok.Data;

@Data
public class ExchangeCreateDTO {
    private Long initiatorBookId;
    private Long myBookId;
    private Long targetBookId;
    private Long targetUserId;
    private String message;

    public Long getEffectiveInitiatorBookId() {
        return initiatorBookId != null ? initiatorBookId : myBookId;
    }
}
