package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.ExploreConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExploreConversationRepository extends JpaRepository<ExploreConversation, Long> {
    List<ExploreConversation> findByUserIdOrderByLastMessageTimeDesc(Long userId);
}
