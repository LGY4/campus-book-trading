package com.booktrading.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class BookVO {
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;
    private String bookCondition;
    private String description;
    private String coverImage;
    private String imagesJson;
    private List<String> images;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private BigDecimal sellerReputation;
    private Boolean isFavorited;
    private Boolean isWanted;
    private Integer quantity;
    private String status;
    private String rejectReason;
    private Integer viewCount;
    private Integer wantCount;
    private LocalDateTime createTime;
    private Long interactionId;
    private LocalDateTime viewTime;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void parseImages() {
        if (imagesJson != null && !imagesJson.isEmpty()) {
            try {
                this.images = MAPPER.readValue(imagesJson, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                this.images = Collections.emptyList();
            }
        } else {
            this.images = Collections.emptyList();
        }
    }
}
