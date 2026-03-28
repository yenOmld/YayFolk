package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.UserProfileVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileVisitRepository extends JpaRepository<UserProfileVisit, Long> {
    Optional<UserProfileVisit> findByViewerIdAndProfileUserId(Long viewerId, Long profileUserId);

    List<UserProfileVisit> findByViewerIdOrderByUpdateTimeDesc(Long viewerId);

    List<UserProfileVisit> findByProfileUserIdOrderByUpdateTimeDesc(Long profileUserId);
}
