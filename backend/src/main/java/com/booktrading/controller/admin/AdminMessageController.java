package com.booktrading.controller.admin;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.PageResult;
import com.booktrading.dto.Result;
import com.booktrading.entity.Message;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.service.MessageService;
import com.booktrading.service.UserService;
import com.booktrading.vo.MessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/message")
public class AdminMessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String type) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 1);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Message::getContent, keyword);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Message::getType, type);
        }
        wrapper.orderByDesc(Message::getCreateTime);
        IPage<Message> msgPage = messageService.page(new Page<>(page, size), wrapper);

        // Batch-fetch users to avoid N+1
        Set<Long> userIds = new HashSet<>();
        msgPage.getRecords().forEach(msg -> {
            if (msg.getSenderId() != null && msg.getSenderId() > 0) userIds.add(msg.getSenderId());
            if (msg.getReceiverId() != null) userIds.add(msg.getReceiverId());
        });
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        IPage<MessageVO> voPage = msgPage.convert(msg -> {
            MessageVO vo = new MessageVO();
            vo.setId(msg.getId());
            vo.setSenderId(msg.getSenderId());
            vo.setReceiverId(msg.getReceiverId());
            vo.setContent(msg.getContent());
            vo.setType(msg.getType());
            vo.setIsRead(msg.getIsRead());
            vo.setCreateTime(msg.getCreateTime());
            if (msg.getSenderId() != null && msg.getSenderId() > 0 && userMap.containsKey(msg.getSenderId())) {
                vo.setSenderName(userMap.get(msg.getSenderId()).getNickname());
            } else {
                vo.setSenderName("系统");
            }
            if (msg.getReceiverId() != null && userMap.containsKey(msg.getReceiverId())) {
                vo.setReceiverName(userMap.get(msg.getReceiverId()).getNickname());
            }
            return vo;
        });
        return Result.ok(PageResult.from(voPage));
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "消息管理", action = "删除消息")
    public Result<?> delete(@PathVariable Long id) {
        Message msg = messageService.getById(id);
        if (msg == null) throw new BusinessException("消息不存在");
        messageService.removeById(id);
        return Result.ok("删除成功");
    }

    @PostMapping("/send")
    @LogOperation(module = "消息管理", action = "发送消息")
    public Result<?> send(@RequestBody Map<String, Object> body) {
        if (body.get("receiverId") == null) throw new BusinessException("缺少接收者ID");
        if (body.get("content") == null || ((String) body.get("content")).trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        Long receiverId;
        try {
            receiverId = Long.valueOf(body.get("receiverId").toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("接收者ID格式不正确");
        }
        String content = (String) body.get("content");
        User receiver = userService.getById(receiverId);
        if (receiver == null) throw new BusinessException("接收用户不存在");
        messageService.sendSystemMessage(receiverId, content);
        return Result.ok("发送成功");
    }

    @PostMapping("/broadcast")
    @LogOperation(module = "消息管理", action = "广播消息")
    public Result<?> broadcast(@RequestBody Map<String, Object> body) {
        if (body.get("content") == null || ((String) body.get("content")).trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        String content = (String) body.get("content");
        List<User> users = userService.list();
        for (User user : users) {
            messageService.sendSystemMessage(user.getId(), content);
        }
        return Result.ok("已发送给 " + users.size() + " 位用户");
    }
}
