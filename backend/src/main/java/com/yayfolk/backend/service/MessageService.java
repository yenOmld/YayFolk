package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Conversation;
import com.yayfolk.backend.entity.Message;
import com.yayfolk.backend.entity.Notification;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ConversationRepository;
import com.yayfolk.backend.repository.MessageRepository;
import com.yayfolk.backend.repository.NotificationRepository;
import com.yayfolk.backend.repository.UserFollowRepository;
import com.yayfolk.backend.repository.UserRepository;
import com.yayfolk.backend.util.TextRepairUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final TranslateService translateService;

    public MessageService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          NotificationRepository notificationRepository,
                          UserFollowRepository userFollowRepository,
                          UserRepository userRepository,
                          StringRedisTemplate redisTemplate,
                          TranslateService translateService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.translateService = translateService;
    }

    public List<Map<String, Object>> getConversationList(String username) {
        User currentUser = findUser(username);
        Long userId = currentUser.getId();
        List<Map<String, Object>> result = new ArrayList<>();

        result.add(createSystemConversation("comment", "\u8BC4\u8BBA\u901A\u77E5", "\u8BC4\u8BBA\u901A\u77E5", getUnreadCommentCount(userId)));
        result.add(createSystemConversation("collection", "\u6536\u85CF\u901A\u77E5", "\u6536\u85CF\u901A\u77E5", getUnreadCollectionCount(userId)));

        List<Conversation> directConversations = conversationRepository.findDirectConversationsByUserId(userId);
        for (Conversation conversation : directConversations) {
            Long otherUserId = userId.equals(conversation.getUser1Id()) ? conversation.getUser2Id() : conversation.getUser1Id();
            User otherUser = userRepository.findById(otherUserId).orElse(null);
            int unreadCount = userId.equals(conversation.getUser1Id())
                ? safeInt(conversation.getUnreadCountUser1())
                : safeInt(conversation.getUnreadCountUser2());
            String type = defaultString(conversation.getType());

            Map<String, Object> item = new HashMap<>();
            item.put("id", conversation.getId());
            item.put("type", type);
            item.put("name", visibleText(conversationName(currentUser, otherUser, type)));
            item.put("avatar", avatarOf(otherUser));
            item.put("lastMessage", visibleText(conversation.getLastMessage()));
            item.put("lastMessageTime", formatDate(conversation.getLastMessageTime()));
            item.put("unreadCount", unreadCount);
            item.put("otherUserId", otherUserId);
            item.put("otherUserName", displayName(otherUser));
            item.put("otherUsername", otherUser == null ? "" : defaultString(otherUser.getUsername()));
            item.put("otherRole", otherUser == null ? "" : defaultString(otherUser.getRole()));
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
            .filter(notification -> !Boolean.TRUE.equals(notification.getIsRead()))
            .count();
    }

    private int getUnreadCollectionCount(Long userId) {
        return (int) notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, "collection")
            .stream()
            .filter(notification -> !Boolean.TRUE.equals(notification.getIsRead()))
            .count();
    }

    @Transactional
    public Map<String, Object> getOrCreateConversation(String username, Long otherUserId) {
        User currentUser = findUser(username);
        Long userId = currentUser.getId();
        if (userId.equals(otherUserId)) {
            throw new RuntimeException("Cannot create a conversation with yourself");
        }

        ensureFollowing(userId, otherUserId);

        Conversation conversation = conversationRepository.findChatConversation(userId, otherUserId)
            .orElseGet(() -> {
                Conversation newConversation = new Conversation();
                newConversation.setType("chat");
                newConversation.setUser1Id(userId);
                newConversation.setUser2Id(otherUserId);
                return conversationRepository.save(newConversation);
            });

        User otherUser = userRepository.findById(otherUserId).orElse(null);
        return buildConversationDetail(currentUser, conversation, otherUser);
    }

    @Transactional
    public Map<String, Object> getOrCreateCustomerServiceConversation(String username) {
        User currentUser = findUser(username);
        Long userId = currentUser.getId();

        List<Conversation> existingServiceConversations = conversationRepository.findServiceConversationsByUserId(userId);
        if (!existingServiceConversations.isEmpty()) {
            Conversation conversation = existingServiceConversations.get(0);
            Long otherUserId = userId.equals(conversation.getUser1Id()) ? conversation.getUser2Id() : conversation.getUser1Id();
            User otherUser = userRepository.findById(otherUserId).orElse(null);
            return buildConversationDetail(currentUser, conversation, otherUser);
        }

        User serviceAdmin = findAvailableServiceAdmin(userId);
        Conversation conversation = conversationRepository.findServiceConversation(userId, serviceAdmin.getId())
            .orElseGet(() -> {
                Conversation newConversation = new Conversation();
                newConversation.setType("service");
                newConversation.setUser1Id(userId);
                newConversation.setUser2Id(serviceAdmin.getId());
                return conversationRepository.save(newConversation);
            });

        return buildConversationDetail(currentUser, conversation, serviceAdmin);
    }

    @Transactional
    public Map<String, Object> sendMessage(String username, Long conversationId, String content) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation does not exist"));

        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new RuntimeException("You do not have permission to send messages in this conversation");
        }

        Long receiverId = userId.equals(conversation.getUser1Id()) ? conversation.getUser2Id() : conversation.getUser1Id();
        if (requiresFollowing(conversation)) {
            ensureFollowing(userId, receiverId);
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType("text");
        message.setSourceLang(detectLanguage(content));
        message.setIsRead(false);
        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessage(content);
        conversation.setLastMessageTime(new Date());
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountUser2(safeInt(conversation.getUnreadCountUser2()) + 1);
        } else {
            conversation.setUnreadCountUser1(safeInt(conversation.getUnreadCountUser1()) + 1);
        }
        conversationRepository.save(conversation);

        return toMessageMap(savedMessage, userId);
    }

    public List<Map<String, Object>> getMessages(String username, Long conversationId) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation does not exist"));

        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new RuntimeException("You do not have permission to view this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
        return messages.stream()
            .filter(message -> {
                if (userId.equals(message.getSenderId())) {
                    return !Boolean.TRUE.equals(message.getDeletedBySender());
                }
                return !Boolean.TRUE.equals(message.getDeletedByReceiver());
            })
            .map(message -> toMessageMap(message, userId))
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(String username, Long conversationId) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation does not exist"));

        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountUser1(0);
        } else if (userId.equals(conversation.getUser2Id())) {
            conversation.setUnreadCountUser2(0);
        }
        conversationRepository.save(conversation);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
        for (Message message : messages) {
            if (userId.equals(message.getReceiverId()) && !Boolean.TRUE.equals(message.getIsRead())) {
                message.setIsRead(true);
                messageRepository.save(message);
            }
        }
    }

    public List<Map<String, Object>> getNotifications(String username, String type) {
        Long userId = findUserId(username);
        List<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, type);

        Set<Long> fromUserIds = notifications.stream()
            .map(Notification::getFromUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
        Map<Long, User> userMap = userRepository.findAllById(fromUserIds)
            .stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        return notifications.stream()
            .map(notification -> toNotificationMap(notification, userMap))
            .collect(Collectors.toList());
    }

    @Transactional
    public void markNotificationsAsRead(String username, String type) {
        Long userId = findUserId(username);
        List<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, type);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
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
        map.put("content", visibleText(message.getContent()));
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
        map.put("content", visibleText(notification.getContent()));
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
        return findUser(username).getId();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private void ensureFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new RuntimeException("Invalid contact target");
        }
        if (followerId.equals(followingId)) {
            return;
        }
        if (!userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("Please follow this user before contacting them");
        }
    }

    private boolean requiresFollowing(Conversation conversation) {
        return "chat".equals(defaultString(conversation.getType()));
    }

    private String displayName(User user) {
        if (user == null) {
            return "Unknown user";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return visibleText(user.getNickname());
        }
        return visibleText(user.getUsername());
    }

    private String conversationName(User currentUser, User otherUser, String type) {
        if ("service".equals(type) && !isAdmin(currentUser)) {
            return "\u5728\u7EBF\u5BA2\u670D";
        }
        return displayName(otherUser);
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    private User findAvailableServiceAdmin(Long requesterId) {
        return userRepository.findByRoleAndStatusAndIsSuperAdminOrderByCreateTimeAsc("admin", 1, 0)
            .stream()
            .filter(admin -> admin.getId() != null && !admin.getId().equals(requesterId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("\u6682\u65E0\u53EF\u7528\u5BA2\u670D\uFF0C\u8BF7\u7A0D\u540E\u518D\u8BD5"));
    }

    private Map<String, Object> buildConversationDetail(User currentUser, Conversation conversation, User otherUser) {
        Long otherUserId = currentUser.getId().equals(conversation.getUser1Id())
            ? conversation.getUser2Id()
            : conversation.getUser1Id();
        Map<String, Object> result = new HashMap<>();
        result.put("id", conversation.getId());
        result.put("type", defaultString(conversation.getType()));
        result.put("otherUserId", otherUserId);
        result.put("otherUserName", displayName(otherUser));
        result.put("otherUserAvatar", avatarOf(otherUser));
        result.put("name", conversationName(currentUser, otherUser, defaultString(conversation.getType())));
        return result;
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

    private String visibleText(String value) {
        return TextRepairUtils.repairIfNeeded(defaultString(value));
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

        List<Conversation> directConversations = conversationRepository.findDirectConversationsByUserId(userId);
        for (Conversation conversation : directConversations) {
            if (userId.equals(conversation.getUser1Id())) {
                total += safeInt(conversation.getUnreadCountUser1());
            } else {
                total += safeInt(conversation.getUnreadCountUser2());
            }
        }

        return total;
    }

    @Transactional
    public void deleteConversation(String username, Long conversationId) {
        Long userId = findUserId(username);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation does not exist"));

        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new RuntimeException("You do not have permission to delete this conversation");
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
            .orElseThrow(() -> new RuntimeException("Message does not exist"));

        if (!userId.equals(message.getSenderId()) && !userId.equals(message.getReceiverId())) {
            throw new RuntimeException("You do not have permission to delete this message");
        }

        Long conversationId = message.getConversationId();
        boolean wasLastMessage = false;

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null && Objects.equals(message.getContent(), conversation.getLastMessage())) {
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

        for (Message message : messages) {
            boolean visibleToUser = userId.equals(message.getSenderId())
                ? !Boolean.TRUE.equals(message.getDeletedBySender())
                : !Boolean.TRUE.equals(message.getDeletedByReceiver());

            if (visibleToUser) {
                newLastMessage = message.getContent();
                newLastMessageTime = message.getCreateTime();
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
            .orElseThrow(() -> new RuntimeException("Message does not exist"));

        if (!userId.equals(message.getSenderId())) {
            throw new RuntimeException("You can only recall messages sent by yourself");
        }

        long timeDiff = System.currentTimeMillis() - message.getCreateTime().getTime();
        if (timeDiff > 2 * 60 * 1000) {
            throw new RuntimeException("Messages can only be recalled within 2 minutes");
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
        result.put("content", visibleText(content));
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
            .orElseThrow(() -> new RuntimeException("Message does not exist"));

        if (!userId.equals(message.getSenderId()) && !userId.equals(message.getReceiverId())) {
            throw new RuntimeException("You do not have permission to translate this message");
        }

        String sourceLang = message.getSourceLang();
        String content = visibleText(message.getContent());
        if (sourceLang == null || "auto".equals(sourceLang)) {
            sourceLang = detectLanguage(content);
        }

        com.yayfolk.backend.dto.TranslateRequest request = new com.yayfolk.backend.dto.TranslateRequest();
        request.setText(content);
        request.setSourceLang(sourceLang);
        request.setTargetLang(targetLang);

        com.yayfolk.backend.dto.TranslateResponse response = translateService.translate(request);
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
