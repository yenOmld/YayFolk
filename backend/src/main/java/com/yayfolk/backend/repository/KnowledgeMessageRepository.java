package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.KnowledgeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeMessageRepository extends JpaRepository<KnowledgeMessage, Long> {
    List<KnowledgeMessage> findByConversationIdOrderByCreateTimeAsc(Long conversationId);
}
