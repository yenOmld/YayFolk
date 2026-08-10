package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.DiscoverPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DiscoverPostRepository extends JpaRepository<DiscoverPost, Long> {
    List<DiscoverPost> findByStatusOrderByCreateTimeDesc(Integer status);

    List<DiscoverPost> findByStatusAndAuditStatusOrderByCreateTimeDesc(Integer status, String auditStatus);

    List<DiscoverPost> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

    List<DiscoverPost> findByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Integer status);

    List<DiscoverPost> findByAuditStatusOrderByCreateTimeDesc(String auditStatus);

    List<DiscoverPost> findByStatusAndAuditStatusInOrderByCreateTimeDesc(Integer status, Collection<String> auditStatuses);

    List<DiscoverPost> findByActivityIdAndStatusAndAuditStatusOrderByCreateTimeDesc(Long activityId, Integer status, String auditStatus);

    List<DiscoverPost> findByActivityIdInAndStatusAndAuditStatusOrderByCreateTimeDesc(List<Long> activityIds, Integer status, String auditStatus);

    List<DiscoverPost> findByActivityIdAndUserIdAndTypeAndStatusAndAuditStatusOrderByCreateTimeDesc(Long activityId, Long userId, String type, Integer status, String auditStatus);

    long countByActivityIdInAndTypeAndStatusAndAuditStatus(List<Long> activityIds, String type, Integer status, String auditStatus);
}
