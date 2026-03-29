package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    boolean existsByPostIdAndReporterIdAndStatus(Long postId, Long reporterId, String status);

    long countByPostIdAndStatus(Long postId, String status);

    List<PostReport> findByPostIdAndStatus(Long postId, String status);

    List<PostReport> findByPostIdInAndStatus(Collection<Long> postIds, String status);
}
