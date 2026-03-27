package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.ActivityBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityBookingRepository extends JpaRepository<ActivityBooking, Long> {
    List<ActivityBooking> findByActivityIdOrderByCreateTimeDesc(Long activityId);
    List<ActivityBooking> findByUserIdOrderByCreateTimeDesc(Long userId);
    Optional<ActivityBooking> findByActivityIdAndUserId(Long activityId, Long userId);
    long countByActivityId(Long activityId);
}
