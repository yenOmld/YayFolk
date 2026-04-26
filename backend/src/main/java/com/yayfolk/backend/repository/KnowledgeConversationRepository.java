package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.KnowledgeConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeConversationRepository extends JpaRepository<KnowledgeConversation, Long> {
    List<KnowledgeConversation> findByUserIdOrderByLastMessageTimeDesc(Long userId);
}
