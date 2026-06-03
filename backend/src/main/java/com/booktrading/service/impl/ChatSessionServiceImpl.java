package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.ChatSession;
import com.booktrading.mapper.ChatSessionMapper;
import com.booktrading.service.ChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    @Override
    @Transactional
    public ChatSession getOrCreateSession(Long user1Id, Long user2Id) {
        Long smaller = Math.min(user1Id, user2Id);
        Long larger = Math.max(user1Id, user2Id);

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUser1Id, smaller)
                .eq(ChatSession::getUser2Id, larger);
        ChatSession session = getOne(wrapper);

        if (session == null) {
            session = new ChatSession();
            session.setUser1Id(smaller);
            session.setUser2Id(larger);
            session.setCreateTime(LocalDateTime.now());
            save(session);
        }

        return session;
    }

    @Override
    public List<ChatSession> getMySessions(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatSession::getUser1Id, userId)
                .or().eq(ChatSession::getUser2Id, userId));
        wrapper.orderByDesc(ChatSession::getLastMessageTime);
        return list(wrapper);
    }

    @Override
    public void updateLastMessage(Long sessionId, String message) {
        LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatSession::getId, sessionId)
                .set(ChatSession::getLastMessage, message)
                .set(ChatSession::getLastMessageTime, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    public void updateLastMessage(Long user1Id, Long user2Id, String message) {
        Long smaller = Math.min(user1Id, user2Id);
        Long larger = Math.max(user1Id, user2Id);
        LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatSession::getUser1Id, smaller)
                .eq(ChatSession::getUser2Id, larger)
                .set(ChatSession::getLastMessage, message)
                .set(ChatSession::getLastMessageTime, LocalDateTime.now());
        update(wrapper);
    }
}
