package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.ActivityReserveParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityReserveParticipantRepository extends JpaRepository<ActivityReserveParticipant, Long> {
    List<ActivityReserveParticipant> findByReserveId(Long reserveId);
    
    void deleteByReserveId(Long reserveId);
}