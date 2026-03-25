package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.DiscoverPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoverPostRepository extends JpaRepository<DiscoverPost, Long> {
    List<DiscoverPost> findByStatusOrderByCreateTimeDesc(Integer status);

    List<DiscoverPost> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

    List<DiscoverPost> findByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Integer status);
}
