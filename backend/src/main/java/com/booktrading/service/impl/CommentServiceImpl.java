package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.dto.CommentCreateDTO;
import com.booktrading.entity.Comment;
import com.booktrading.entity.Order;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.CommentMapper;
import com.booktrading.service.CommentService;
import com.booktrading.service.MessageService;
import com.booktrading.service.OrderService;
import com.booktrading.service.UserService;
import com.booktrading.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional
    public void createComment(CommentCreateDTO dto, Long userId) {
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        Order order = orderService.getById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权评价此订单");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("订单未完成，无法评价");
        }

        long existing = count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getOrderId, dto.getOrderId()));
        if (existing > 0) {
            throw new BusinessException("该订单已评价");
        }

        Comment comment = new Comment();
        comment.setOrderId(dto.getOrderId());
        comment.setBookId(order.getBookId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setRating(dto.getRating());
        comment.setImagesJson(dto.getImages());
        save(comment);

        recalculateReputation(order.getSellerId());
        messageService.sendTradeMessage(order.getSellerId(),
                "您的订单 " + order.getOrderNo() + " 收到新的评价，评分：" + dto.getRating() + "星");
    }

    @Override
    @Transactional
    public void createFollowUp(CommentCreateDTO dto, Long userId) {
        if (dto.getParentId() == null) {
            throw new BusinessException("缺少追评目标评论ID");
        }
        Comment parent = getById(dto.getParentId());
        if (parent == null) {
            throw new BusinessException("原评论不存在");
        }
        if (!parent.getUserId().equals(userId)) {
            throw new BusinessException("只能追评自己的评论");
        }
        if (parent.getParentId() != null) {
            throw new BusinessException("不能对追评进行追评");
        }

        Comment followUp = new Comment();
        followUp.setOrderId(parent.getOrderId());
        followUp.setParentId(parent.getId());
        followUp.setBookId(parent.getBookId());
        followUp.setUserId(userId);
        followUp.setContent(dto.getContent());
        followUp.setRating(parent.getRating());
        followUp.setImagesJson(dto.getImages());
        save(followUp);

        Order order = orderService.getById(parent.getOrderId());
        if (order != null) {
            messageService.sendTradeMessage(order.getSellerId(),
                    "订单 " + order.getOrderNo() + " 收到追评");
        }
    }

    @Override
    public IPage<CommentVO> getBookComments(Long bookId, int page, int size) {
        // 只查顶层评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBookId, bookId);
        wrapper.isNull(Comment::getParentId);
        wrapper.orderByDesc(Comment::getCreateTime);
        IPage<Comment> commentPage = page(new Page<>(page, size), wrapper);

        // 批量查询追评
        Set<Long> parentIds = commentPage.getRecords().stream()
                .map(Comment::getId).collect(Collectors.toSet());
        Map<Long, List<CommentVO>> followUpMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            LambdaQueryWrapper<Comment> fuWrapper = new LambdaQueryWrapper<>();
            fuWrapper.in(Comment::getParentId, parentIds);
            fuWrapper.orderByAsc(Comment::getCreateTime);
            List<Comment> followUps = list(fuWrapper);
            for (Comment fu : followUps) {
                followUpMap.computeIfAbsent(fu.getParentId(), k -> new ArrayList<>())
                        .add(toCommentVO(fu));
            }
        }

        IPage<CommentVO> result = commentPage.convert(this::toCommentVO);
        result.getRecords().forEach(vo -> vo.setFollowUps(
                followUpMap.getOrDefault(vo.getId(), Collections.emptyList())));
        return result;
    }

    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setParentId(comment.getParentId());
        vo.setBookId(comment.getBookId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setRating(comment.getRating());
        vo.setImagesJson(comment.getImagesJson());
        vo.setCreateTime(comment.getCreateTime());

        User user = userService.getById(comment.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }
        return vo;
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long userId) {
        Comment comment = getById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此评论");
        }

        Order order = orderService.getById(comment.getOrderId());
        removeById(id);

        if (order != null) {
            recalculateReputation(order.getSellerId());
        }
    }

    @Override
    @Transactional
    public void adminDeleteComment(Long id) {
        Comment comment = getById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        Order order = orderService.getById(comment.getOrderId());
        removeById(id);
        if (order != null) {
            recalculateReputation(order.getSellerId());
        }
    }

    @Override
    public Double getSellerRating(Long sellerId) {
        List<Long> orderIds = getCompletedOrderIds(sellerId);
        if (orderIds.isEmpty()) return 5.0;

        List<Comment> comments = list(new LambdaQueryWrapper<Comment>()
                .in(Comment::getOrderId, orderIds));

        if (comments.isEmpty()) {
            return 5.0;
        }

        double avg = comments.stream()
                .mapToInt(Comment::getRating)
                .average()
                .orElse(5.0);

        return BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private void recalculateReputation(Long sellerId) {
        List<Long> orderIds = getCompletedOrderIds(sellerId);
        if (orderIds.isEmpty()) {
            User seller = userService.getById(sellerId);
            if (seller != null) {
                seller.setReputationScore(BigDecimal.valueOf(5.0));
                userService.updateById(seller);
            }
            return;
        }

        List<Comment> comments = list(new LambdaQueryWrapper<Comment>()
                .in(Comment::getOrderId, orderIds));

        User seller = userService.getById(sellerId);
        if (seller == null) return;

        if (comments.isEmpty()) {
            seller.setReputationScore(BigDecimal.valueOf(5.0));
        } else {
            double avg = comments.stream()
                    .mapToInt(Comment::getRating)
                    .average()
                    .orElse(5.0);
            seller.setReputationScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }
        userService.updateById(seller);
    }

    private List<Long> getCompletedOrderIds(Long sellerId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerId, sellerId)
                .notIn(Order::getStatus, "REFUNDED", "DISPUTE", "CANCELLED");
        wrapper.select(Order::getId);
        return orderService.list(wrapper).stream()
                .map(Order::getId)
                .collect(java.util.stream.Collectors.toList());
    }
}
