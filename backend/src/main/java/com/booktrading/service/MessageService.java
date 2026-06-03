package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.Message;

public interface MessageService extends IService<Message> {

    void sendSystemMessage(Long receiverId, String content);

    void sendTradeMessage(Long receiverId, String content);

    void sendTradeMessage(Long receiverId, Long senderId, String content);

    IPage<Message> getMyMessages(Long userId, int page, int size);

    IPage<Message> getMyMessages(Long userId, int page, int size, String type);

    void markAsRead(Long messageId, Long userId);

    void markAllAsRead(Long userId);

    void markAsReadBySender(Long receiverId, Long senderId);

    void deleteMessage(Long messageId, Long userId);

    int getUnreadCount(Long userId);

    int getUnreadCountBySender(Long receiverId, Long senderId);
}
