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

    @Query("SELECT c FROM Conversation c WHERE c.type = 'chat' AND (c.user1Id = :userId OR c.user2Id = :userId) ORDER BY c.lastMessageTime DESC")
    List<Conversation> findChatConversationsByUserId(@Param("userId") Long userId);

    Optional<Conversation> findByType(String type);
}
