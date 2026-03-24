package com.travelate.backend.repository;

import com.travelate.backend.entity.DiscoverPostHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscoverPostHistoryRepository extends JpaRepository<DiscoverPostHistory, Long> {
    Optional<DiscoverPostHistory> findByUserIdAndPostId(Long userId, Long postId);

    List<DiscoverPostHistory> findByUserIdOrderByLastViewTimeDesc(Long userId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByPostId(Long postId);
}
