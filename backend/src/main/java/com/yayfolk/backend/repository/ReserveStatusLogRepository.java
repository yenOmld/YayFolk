package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.ReserveStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReserveStatusLogRepository extends JpaRepository<ReserveStatusLog, Long> {
    List<ReserveStatusLog> findByReserveIdInOrderByCreateTimeAsc(List<Long> reserveIds);

    List<ReserveStatusLog> findByReserveIdOrderByCreateTimeAsc(Long reserveId);
}
