package com.travelate.backend.repository;

import com.travelate.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreateTimeAsc(Long conversationId);

    List<Message> findByConversationIdOrderByCreateTimeDesc(Long conversationId);

    void deleteByConversationId(Long conversationId);
}
