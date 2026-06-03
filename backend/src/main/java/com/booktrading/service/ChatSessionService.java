package com.booktrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.ChatSession;

import java.util.List;

public interface ChatSessionService extends IService<ChatSession> {

    ChatSession getOrCreateSession(Long user1Id, Long user2Id);

    List<ChatSession> getMySessions(Long userId);

    void updateLastMessage(Long sessionId, String message);

    void updateLastMessage(Long user1Id, Long user2Id, String message);
}
