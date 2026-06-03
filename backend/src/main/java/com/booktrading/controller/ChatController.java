package com.booktrading.controller;

import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.ChatSession;
import com.booktrading.entity.Message;
import com.booktrading.entity.User;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.ChatSessionService;
import com.booktrading.service.MessageService;
import com.booktrading.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/sessions")
    public Result<?> getMySessions() {
        Long userId = currentUser.getId();
        List<ChatSession> sessions = chatSessionService.getMySessions(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            if (session.getUser1Id() == null || session.getUser2Id() == null) continue;
            Long otherUserId = session.getUser1Id().equals(userId) ? session.getUser2Id() : session.getUser1Id();
            User other = userService.getById(otherUserId);
            if (other == null) continue;

            int unread = messageService.getUnreadCountBySender(userId, otherUserId);

            Map<String, Object> vo = new HashMap<>();
            vo.put("sessionId", session.getId());
            vo.put("userId", otherUserId);
            vo.put("nickname", other.getNickname());
            vo.put("avatar", other.getAvatar());
            vo.put("lastMessage", session.getLastMessage());
            vo.put("lastTime", session.getLastMessageTime());
            vo.put("unread", unread);
            result.add(vo);
        }
        return Result.ok(result);
    }

    @GetMapping("/history/{sessionId}")
    public Result<?> getChatHistory(@PathVariable Long sessionId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "100") int size) {
        ChatSession session = chatSessionService.getById(sessionId);
        if (session == null) {
            return Result.error("会话不存在");
        }
        Long userId = currentUser.getId();
        if (!userId.equals(session.getUser1Id()) && !userId.equals(session.getUser2Id())) {
            return Result.error("无权查看此会话");
        }
        return Result.ok(getMessagesBetween(session.getUser1Id(), session.getUser2Id(), page, size));
    }

    @PostMapping("/read/{senderId}")
    public Result<?> markAsRead(@PathVariable Long senderId) {
        messageService.markAsReadBySender(currentUser.getId(), senderId);
        return Result.ok();
    }

    @GetMapping("/history/user/{userId}")
    public Result<?> getChatHistoryByUser(@PathVariable Long userId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "100") int size) {
        Long currentUserId = currentUser.getId();
        ChatSession session = chatSessionService.getOrCreateSession(currentUserId, userId);
        return Result.ok(getMessagesBetween(currentUserId, userId, page, size));
    }

    private PageResult getMessagesBetween(Long user1Id, Long user2Id, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w2 -> w2.eq(Message::getSenderId, user1Id).eq(Message::getReceiverId, user2Id))
                .or(w2 -> w2.eq(Message::getSenderId, user2Id).eq(Message::getReceiverId, user1Id))
        );
        wrapper.orderByAsc(Message::getCreateTime);
        IPage<Message> result = messageService.page(new Page<>(page, size), wrapper);
        return PageResult.from(result);
    }
}
