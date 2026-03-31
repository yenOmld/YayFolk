package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    List<UserFollow> findByFollowerIdOrderByCreateTimeDesc(Long followerId);

    List<UserFollow> findByFollowingIdOrderByCreateTimeDesc(Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
