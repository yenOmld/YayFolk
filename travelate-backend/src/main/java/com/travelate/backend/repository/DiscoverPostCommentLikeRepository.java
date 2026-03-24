package com.travelate.backend.repository;

import com.travelate.backend.entity.DiscoverPostCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoverPostCommentLikeRepository extends JpaRepository<DiscoverPostCommentLike, Long> {
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    List<DiscoverPostCommentLike> findByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);

    void deleteByCommentId(Long commentId);

    void deleteByCommentIdIn(List<Long> commentIds);
}
