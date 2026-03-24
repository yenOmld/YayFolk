package com.travelate.backend.repository;

import com.travelate.backend.entity.DiscoverPostCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoverPostCollectionRepository extends JpaRepository<DiscoverPostCollection, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    List<DiscoverPostCollection> findByUserIdAndPostIdIn(Long userId, List<Long> postIds);

    List<DiscoverPostCollection> findByUserIdOrderByCreateTimeDesc(Long userId);

    long countByUserId(Long userId);

    void deleteByPostId(Long postId);
}
