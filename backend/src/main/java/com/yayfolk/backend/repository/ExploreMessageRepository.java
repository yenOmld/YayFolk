package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.ExploreMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExploreMessageRepository extends JpaRepository<ExploreMessage, Long> {
    List<ExploreMessage> findByConversationIdOrderByCreateTimeAsc(Long conversationId);
}
