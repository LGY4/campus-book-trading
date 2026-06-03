package com.booktrading.websocket;

import com.booktrading.entity.Message;
import com.booktrading.mapper.MessageMapper;
import com.booktrading.service.ChatSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.put(userId, session);
            log.info("用户{}建立WebSocket连接", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = getUserIdFromSession(session);
        if (senderId == null) return;

        try {
            Map<String, Object> msgMap = objectMapper.readValue(message.getPayload(), Map.class);
            Object receiverIdObj = msgMap.get("receiverId");
            Object contentObj = msgMap.get("content");
            if (receiverIdObj == null || contentObj == null) {
                log.warn("WebSocket消息缺少receiverId或content");
                return;
            }
            Long receiverId = Long.valueOf(receiverIdObj.toString());
            String content = contentObj.toString().trim();
            if (content.isEmpty()) return;

            // 消息类型：默认CHAT，支持IMAGE
            Object typeObj = msgMap.get("type");
            String msgType = (typeObj != null && "IMAGE".equals(typeObj.toString())) ? "IMAGE" : "CHAT";
            String displayMsg = "IMAGE".equals(msgType) ? "[图片]" : content;

            // 保存消息到数据库
            Message chatMsg = new Message();
            chatMsg.setSenderId(senderId);
            chatMsg.setReceiverId(receiverId);
            chatMsg.setContent(content);
            chatMsg.setType(msgType);
            chatMsg.setIsRead(0);
            chatMsg.setCreateTime(LocalDateTime.now());
            messageMapper.insert(chatMsg);

            // 更新会话最后消息
            chatSessionService.getOrCreateSession(senderId, receiverId);
            chatSessionService.updateLastMessage(senderId, receiverId, displayMsg);

            // 构造发送给接收者的消息
            String jsonMsg = objectMapper.writeValueAsString(Map.of(
                    "id", chatMsg.getId(),
                    "senderId", senderId,
                    "receiverId", receiverId,
                    "content", content,
                    "type", msgType,
                    "createTime", chatMsg.getCreateTime().toString()
            ));

            // 发送给接收者
            WebSocketSession receiverSession = sessions.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.sendMessage(new TextMessage(jsonMsg));
            }

            // 回显给发送者
            session.sendMessage(new TextMessage(jsonMsg));
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("用户{}断开WebSocket连接", userId);
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("currentUserId");
        if (userId instanceof Long) return (Long) userId;
        if (userId != null) return Long.valueOf(userId.toString());
        return null;
    }
}
