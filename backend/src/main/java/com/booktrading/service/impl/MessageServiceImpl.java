package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.Message;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.MessageMapper;
import com.booktrading.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    @Transactional
    public void sendSystemMessage(Long receiverId, String content) {
        Message message = new Message();
        message.setSenderId(null);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType("SYSTEM");
        message.setIsRead(0);
        save(message);
    }

    @Override
    @Transactional
    public void sendTradeMessage(Long receiverId, String content) {
        sendTradeMessage(receiverId, null, content);
    }

    @Override
    @Transactional
    public void sendTradeMessage(Long receiverId, Long senderId, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType("TRADE");
        message.setIsRead(0);
        save(message);
    }

    @Override
    public IPage<Message> getMyMessages(Long userId, int page, int size) {
        return getMyMessages(userId, page, size, null);
    }

    @Override
    public IPage<Message> getMyMessages(Long userId, int page, int size, String type) {
        size = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Message::getType, type);
        }
        wrapper.orderByDesc(Message::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        update(new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, messageId)
                .eq(Message::getReceiverId, userId)
                .set(Message::getIsRead, 1));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        update(new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }

    @Override
    @Transactional
    public void markAsReadBySender(Long receiverId, Long senderId) {
        update(new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getSenderId, senderId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = getById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (!message.getReceiverId().equals(userId)) {
            throw new BusinessException("无权删除此消息");
        }
        removeById(messageId);
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0);
        return (int) count(wrapper);
    }

    @Override
    public int getUnreadCountBySender(Long receiverId, Long senderId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .eq(Message::getSenderId, senderId)
                .eq(Message::getIsRead, 0);
        return (int) count(wrapper);
    }
}
