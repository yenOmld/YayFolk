package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.MerchantReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantReviewRepository extends JpaRepository<MerchantReview, Long> {
    List<MerchantReview> findByMerchantIdOrderByCreateTimeDesc(Long merchantId);

    Optional<MerchantReview> findFirstByReserveIdAndUserIdOrderByCreateTimeDesc(Long reserveId, Long userId);

    Optional<MerchantReview> findFirstByReserveIdOrderByCreateTimeDesc(Long reserveId);

    @Query("SELECT AVG(mr.score) FROM MerchantReview mr WHERE mr.merchantId = :merchantId")
    Double getAverageScoreByMerchantId(Long merchantId);

    @Query("SELECT COUNT(mr) FROM MerchantReview mr WHERE mr.merchantId = :merchantId")
    Long countByMerchantId(Long merchantId);
}
