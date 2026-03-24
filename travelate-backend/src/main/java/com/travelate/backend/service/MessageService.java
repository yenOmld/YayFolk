package com.travelate.backend.service;

import com.travelate.backend.entity.Conversation;
import com.travelate.backend.entity.Message;
import com.travelate.backend.entity.Notification;
import com.travelate.backend.entity.User;
import com.travelate.backend.repository.ConversationRepository;
import com.travelate.backend.repository.MessageRepository;
import com.travelate.backend.repository.NotificationRepository;
import com.travelate.backend.repository.UserRepository;
import com.travelate.backend.service.TranslateService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final TranslateService translateService;

    public MessageService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          NotificationRepository notificationRepository,
                          UserRepository userRepository,
                          StringRedisTemplate redisTemplate,
                          TranslateService translateService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.translateService = translateService;
    }

    public List<Map<String, Object>> getConversationList(String username) {
        Long userId = findUserId(username);
        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, Object> commentNotif = createSystemConversation("comment", "评论通知", "评论通知", getUnreadCommentCount(userId));
        result.add(commentNotif);

        Map<String, Object> collectNotif = createSystemConversation("collection", "收藏通知", "收藏通知", getUnreadCollectionCount(userId));
        result.add(collectNotif);

        List<Conversation> chatConversations = conversationRepository.findChatConversationsByUserId(userId);
        for (Conversation conv : chatConversations) {
            Long otherUserId = userId.equals(conv.getUser1Id()) ? conv.getUser2Id() : conv.getUser1Id();
            User otherUser = userRepository.findById(otherUserId).orElse(null);
            int unreadCount = userId.equals(conv.getUser1Id()) ? safeInt(conv.getUnreadCountUser1()) : safeInt(conv.getUnreadCountUser2());

            Map<String, Object> item = new HashMap<>();
            item.put("id", conv.getId());
            item.put("type", "chat");
            item.put("name", displayName(otherUser));
            item.put("avatar", avatarOf(otherUser));
            item.put("lastMessage", defaultString(conv.getLastMessage()));
            item.put("lastMessageTime", formatDate(conv.getLastMessageTime()));
            item.put("unreadCount", unreadCount);
            item.put("otherUserId", otherUserId);
            result.add(item);
        }

        return result;
    }

    private Map<String, Object> createSystemConversation(String type, String name, String lastMessage, int unreadCount) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", type);
        item.put("type", type);
        item.put("name", name);
        item.put("avatar", null);
        item.put("lastMessage", lastMessage);
        item.put("lastMessageTime", null);
        item.put("unreadCount", unreadCount);
        item.put("otherUserId", null);
        return item;
    }

    private int getUnreadCommentCount(Long userId) {
        return (int) notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, "comment")
            .stream()
            .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
            .count();
    }

    private int getUnreadCollectionCount(Long userId) {
        return (int) notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, "collection")
            .stream()
            .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
            .count();
    }

    @Transactional
    public Map<String, Object> getOrCreateConversation(String username, Long otherUserId) {
        Long userId = findUserId(username);
        if (userId.equals(otherUserId)) {
            throw new RuntimeException("不能与自己创建会话");
        }

        Conversation conversation = conversationRepository.findChatConversation(userId, otherUserId)
            .orElseGet(() -> {
                Conversation newConv = new Conversation();
                newConv.setType("chat");
                newConv.setUser1Id(userId);
                newConv.setUser2Id(otherUserId);
                return conversationRepository.save(newConv);
            });

        User otherUser = userRepository.findById(otherUserId).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("id", conversation.getId());
        result.put("type", "chat");
        result.put("otherUserId", otherUserId);
        result.put("otherUserName", displayName(otherUser));
        result.put("otherUserAvatar", avatarOf(otherUser));
        return result;
    }

    @Transactional
    public Map<String, Object> sendMessage(String username, Long conversationId, String content) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("会话不存在"));

        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new RuntimeException("无权限发送消息");
        }

        Long receiverId = userId.equals(conversation.getUser1Id()) ? conversation.getUser2Id() : conversation.getUser1Id();

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType("text");
        message.setSourceLang(detectLanguage(content));
        message.setIsRead(false);
        Message saved = messageRepository.save(message);

        conversation.setLastMessage(content);
        conversation.setLastMessageTime(new Date());
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountUser2(safeInt(conversation.getUnreadCountUser2()) + 1);
        } else {
            conversation.setUnreadCountUser1(safeInt(conversation.getUnreadCountUser1()) + 1);
        }
        conversationRepository.save(conversation);

        return toMessageMap(saved, userId);
    }

    public List<Map<String, Object>> getMessages(String username, Long conversationId) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("会话不存在"));

        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new RuntimeException("无权限查看消息");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
        return messages.stream()
            .filter(m -> {
                if (userId.equals(m.getSenderId())) {
                    return !Boolean.TRUE.equals(m.getDeletedBySender());
                } else {
                    return !Boolean.TRUE.equals(m.getDeletedByReceiver());
                }
            })
            .map(m -> toMessageMap(m, userId))
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(String username, Long conversationId) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("会话不存在"));

        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountUser1(0);
        } else if (userId.equals(conversation.getUser2Id())) {
            conversation.setUnreadCountUser2(0);
        }
        conversationRepository.save(conversation);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
        for (Message m : messages) {
            if (userId.equals(m.getReceiverId()) && !Boolean.TRUE.equals(m.getIsRead())) {
                m.setIsRead(true);
                messageRepository.save(m);
            }
        }
    }

    public List<Map<String, Object>> getNotifications(String username, String type) {
        Long userId = findUserId(username);
        List<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, type);

        Set<Long> fromUserIds = notifications.stream()
            .map(Notification::getFromUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(fromUserIds)
            .stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return notifications.stream()
            .map(n -> toNotificationMap(n, userMap))
            .collect(Collectors.toList());
    }

    @Transactional
    public void markNotificationsAsRead(String username, String type) {
        Long userId = findUserId(username);
        List<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, type);
        for (Notification n : notifications) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void createCommentNotification(Long postAuthorId, Long fromUserId, Long postId, Long commentId, String content) {
        if (postAuthorId.equals(fromUserId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(postAuthorId);
        notification.setType("comment");
        notification.setFromUserId(fromUserId);
        notification.setPostId(postId);
        notification.setCommentId(commentId);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createCollectionNotification(Long postAuthorId, Long fromUserId, Long postId, String content) {
        if (postAuthorId.equals(fromUserId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(postAuthorId);
        notification.setType("collection");
        notification.setFromUserId(fromUserId);
        notification.setPostId(postId);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private Map<String, Object> toMessageMap(Message message, Long currentUserId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("content", message.getContent());
        map.put("time", formatDate(message.getCreateTime()));
        map.put("isSelf", currentUserId.equals(message.getSenderId()));
        map.put("isRead", message.getIsRead());
        map.put("sourceLang", message.getSourceLang());
        return map;
    }

    private Map<String, Object> toNotificationMap(Notification notification, Map<Long, User> userMap) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getType());
        map.put("content", notification.getContent());
        map.put("time", formatDate(notification.getCreateTime()));
        map.put("isRead", notification.getIsRead());

        User fromUser = userMap.get(notification.getFromUserId());
        if (fromUser != null) {
            map.put("fromUserName", displayName(fromUser));
            map.put("fromUserAvatar", avatarOf(fromUser));
        }

        if (notification.getPostId() != null) {
            map.put("postId", notification.getPostId());
        }
        if (notification.getCommentId() != null) {
            map.put("commentId", notification.getCommentId());
        }

        return map;
    }

    private Long findUserId(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getId();
    }

    private String displayName(User user) {
        if (user == null) {
            return "未知用户";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return defaultString(user.getUsername());
    }

    private String avatarOf(User user) {
        if (user == null || !StringUtils.hasText(user.getAvatar())) {
            return "https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user";
        }
        return user.getAvatar();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    public int getTotalUnreadCount(String username) {
        Long userId = findUserId(username);
        int total = 0;

        total += getUnreadCommentCount(userId);
        total += getUnreadCollectionCount(userId);

        List<Conversation> chatConversations = conversationRepository.findChatConversationsByUserId(userId);
        for (Conversation conv : chatConversations) {
            if (userId.equals(conv.getUser1Id())) {
                total += safeInt(conv.getUnreadCountUser1());
            } else {
                total += safeInt(conv.getUnreadCountUser2());
            }
        }

        return total;
    }

    @Transactional
    public void deleteConversation(String username, Long conversationId) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("会话不存在"));

        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new RuntimeException("无权限删除此会话");
        }

        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public void clearNotifications(String username, String type) {
        Long userId = findUserId(username);
        List<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, type);
        notificationRepository.deleteAll(notifications);
    }

    @Transactional
    public void deleteMessage(String username, Long messageId) {
        Long userId = findUserId(username);
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (!userId.equals(message.getSenderId()) && !userId.equals(message.getReceiverId())) {
            throw new RuntimeException("无权限删除此消息");
        }

        Long conversationId = message.getConversationId();
        boolean wasLastMessage = false;
        
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null && message.getContent().equals(conversation.getLastMessage())) {
            wasLastMessage = true;
        }

        if (userId.equals(message.getSenderId())) {
            message.setDeletedBySender(true);
        } else {
            message.setDeletedByReceiver(true);
        }

        if (Boolean.TRUE.equals(message.getDeletedBySender()) && Boolean.TRUE.equals(message.getDeletedByReceiver())) {
            messageRepository.delete(message);
        } else {
            messageRepository.save(message);
        }

        if (wasLastMessage && conversation != null) {
            updateConversationLastMessage(conversation, userId);
        }
    }

    private void updateConversationLastMessage(Conversation conversation, Long userId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByCreateTimeDesc(conversation.getId());
        String newLastMessage = "";
        Date newLastMessageTime = null;
        
        for (Message m : messages) {
            boolean visibleToUser = userId.equals(m.getSenderId()) 
                ? !Boolean.TRUE.equals(m.getDeletedBySender())
                : !Boolean.TRUE.equals(m.getDeletedByReceiver());
            
            if (visibleToUser) {
                newLastMessage = m.getContent();
                newLastMessageTime = m.getCreateTime();
                break;
            }
        }
        
        conversation.setLastMessage(newLastMessage);
        conversation.setLastMessageTime(newLastMessageTime);
        conversationRepository.save(conversation);
    }

    @Transactional
    public Map<String, Object> recallMessage(String username, Long messageId) {
        Long userId = findUserId(username);
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (!userId.equals(message.getSenderId())) {
            throw new RuntimeException("只能撤销自己发送的消息");
        }

        long timeDiff = System.currentTimeMillis() - message.getCreateTime().getTime();
        if (timeDiff > 2 * 60 * 1000) {
            throw new RuntimeException("消息发送超过2分钟，无法撤销");
        }

        String redisKey = "recall:message:" + messageId;
        String content = message.getContent();
        Long conversationId = message.getConversationId();
        redisTemplate.opsForValue().set(redisKey, content, 1, TimeUnit.MINUTES);

        messageRepository.delete(message);

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            updateConversationLastMessage(conversation, userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", messageId);
        result.put("content", content);
        result.put("conversationId", conversationId);
        return result;
    }

    public String getRecalledMessage(Long messageId) {
        String redisKey = "recall:message:" + messageId;
        return redisTemplate.opsForValue().get(redisKey);
    }

    public Map<String, Object> translateMessage(String username, Long messageId, String targetLang) {
        Long userId = findUserId(username);
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (!userId.equals(message.getSenderId()) && !userId.equals(message.getReceiverId())) {
            throw new RuntimeException("无权限翻译此消息");
        }

        String sourceLang = message.getSourceLang();
        if (sourceLang == null || "auto".equals(sourceLang)) {
            sourceLang = detectLanguage(message.getContent());
        }

        com.travelate.backend.dto.TranslateRequest request = new com.travelate.backend.dto.TranslateRequest();
        request.setText(message.getContent());
        request.setSourceLang(sourceLang);
        request.setTargetLang(targetLang);

        com.travelate.backend.dto.TranslateResponse response = translateService.translate(request);
        Map<String, Object> result = new HashMap<>();
        result.put("translatedText", response.getTranslatedText());
        result.put("sourceLang", sourceLang);
        result.put("targetLang", targetLang);
        return result;
    }

    private String detectLanguage(String text) {
        if (!StringUtils.hasText(text)) {
            return "auto";
        }
        return detectLanguageByChars(text);
    }

    private String detectLanguageByChars(String text) {
        int zhCount = 0;
        int jaCount = 0;
        int koCount = 0;
        int enCount = 0;
        int total = 0;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c) || Character.isDigit(c)) {
                continue;
            }
            total++;
            if (c >= 0x4E00 && c <= 0x9FFF) {
                zhCount++;
            } else if (c >= 0x3040 && c <= 0x309F) {
                jaCount++;
            } else if (c >= 0x30A0 && c <= 0x30FF) {
                jaCount++;
            } else if (c >= 0xAC00 && c <= 0xD7AF) {
                koCount++;
            } else if (c >= 0x1100 && c <= 0x11FF) {
                koCount++;
            } else if (c >= 0x3130 && c <= 0x318F) {
                koCount++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                enCount++;
            }
        }

        if (total == 0) {
            return "auto";
        }

        if (jaCount > 0 && jaCount >= zhCount * 0.1) {
            return "ja";
        }
        if (koCount > 0) {
            return "ko";
        }
        if (zhCount > 0 && zhCount > enCount) {
            return "zh";
        }
        if (enCount > 0 && enCount > zhCount) {
            return "en";
        }
        if (zhCount > 0) {
            return "zh";
        }
        if (enCount > 0) {
            return "en";
        }
        return "auto";
    }
}
