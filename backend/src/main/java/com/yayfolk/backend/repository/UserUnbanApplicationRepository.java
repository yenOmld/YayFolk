package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.UserUnbanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserUnbanApplicationRepository extends JpaRepository<UserUnbanApplication, Long> {
    List<UserUnbanApplication> findAllByOrderByCreateTimeDesc();

    List<UserUnbanApplication> findByStatusOrderByCreateTimeDesc(String status);

    boolean existsByUserIdAndStatus(Long userId, String status);

    Optional<UserUnbanApplication> findTopByUserIdOrderByCreateTimeDesc(Long userId);
}
