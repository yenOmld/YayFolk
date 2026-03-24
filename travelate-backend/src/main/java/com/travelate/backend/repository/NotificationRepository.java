package com.travelate.backend.repository;

import com.travelate.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type);

    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);

    int countByUserIdAndIsReadFalse(Long userId);

    void deleteByUserId(Long userId);
}
