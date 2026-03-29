package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("SELECT c FROM Conversation c WHERE c.type = 'chat' AND ((c.user1Id = :userId AND c.user2Id = :otherUserId) OR (c.user1Id = :otherUserId AND c.user2Id = :userId))")
    Optional<Conversation> findChatConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    @Query("SELECT c FROM Conversation c WHERE c.type IN ('chat', 'service') AND (c.user1Id = :userId OR c.user2Id = :userId) ORDER BY CASE WHEN c.lastMessageTime IS NULL THEN 1 ELSE 0 END, c.lastMessageTime DESC, c.id DESC")
    List<Conversation> findDirectConversationsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.type = 'service' AND ((c.user1Id = :userId AND c.user2Id = :adminId) OR (c.user1Id = :adminId AND c.user2Id = :userId))")
    Optional<Conversation> findServiceConversation(@Param("userId") Long userId, @Param("adminId") Long adminId);

    @Query("SELECT c FROM Conversation c WHERE c.type = 'service' AND (c.user1Id = :userId OR c.user2Id = :userId) ORDER BY CASE WHEN c.lastMessageTime IS NULL THEN 1 ELSE 0 END, c.lastMessageTime DESC, c.id DESC")
    List<Conversation> findServiceConversationsByUserId(@Param("userId") Long userId);

    Optional<Conversation> findByType(String type);
}