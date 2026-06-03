package com.booktrading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_exchange")
public class Exchange {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long initiatorBookId;

    private Long targetBookId;

    private Long initiatorId;

    private Long targetUserId;

    private String status;

    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
