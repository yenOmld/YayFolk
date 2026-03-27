package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByMerchantIdOrderByCreateTimeDesc(Long merchantId);
    List<Activity> findByStatusOrderByStartTimeAsc(String status);
    List<Activity> findByStatusNotOrderByStartTimeAsc(String status);
    List<Activity> findByAuditStatusOrderByCreateTimeDesc(String auditStatus);
    List<Activity> findByAuditStatusAndStatusNotOrderByStartTimeAsc(String auditStatus, String status);
}
