package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.RoutePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    List<RoutePlan> findByUserIdOrderByUpdateTimeDescIdDesc(Long userId);
    Optional<RoutePlan> findByIdAndUserId(Long id, Long userId);
}
