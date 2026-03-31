package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.ActivityReserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityReserveRepository extends JpaRepository<ActivityReserve, Long> {
    List<ActivityReserve> findByActivityIdOrderByCreateTimeDesc(Long activityId);

    List<ActivityReserve> findByMerchantIdOrderByUpdateTimeDesc(Long merchantId);

    List<ActivityReserve> findByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<ActivityReserve> findFirstByMerchantIdAndReserveNoIgnoreCase(Long merchantId, String reserveNo);

    Optional<ActivityReserve> findFirstByReserveNoIgnoreCase(String reserveNo);

    Optional<ActivityReserve> findFirstByActivityIdAndUserIdOrderByCreateTimeDesc(Long activityId, Long userId);
}
