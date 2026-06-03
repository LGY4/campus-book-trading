package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.dto.CommentCreateDTO;
import com.booktrading.entity.Comment;
import com.booktrading.vo.CommentVO;

public interface CommentService extends IService<Comment> {

    void createComment(CommentCreateDTO dto, Long userId);

    void createFollowUp(CommentCreateDTO dto, Long userId);

    IPage<CommentVO> getBookComments(Long bookId, int page, int size);

    void deleteComment(Long id, Long userId);

    void adminDeleteComment(Long id);

    Double getSellerRating(Long sellerId);
}
