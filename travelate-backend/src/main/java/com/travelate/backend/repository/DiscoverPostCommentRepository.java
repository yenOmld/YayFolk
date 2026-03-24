package com.travelate.backend.repository;

import com.travelate.backend.entity.DiscoverPostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoverPostCommentRepository extends JpaRepository<DiscoverPostComment, Long> {
    List<DiscoverPostComment> findByPostIdOrderByCreateTimeAsc(Long postId);

    void deleteByPostId(Long postId);
}
