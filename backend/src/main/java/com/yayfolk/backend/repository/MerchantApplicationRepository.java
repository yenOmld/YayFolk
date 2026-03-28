package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.MerchantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication, Long> {
    Optional<MerchantApplication> findByUserId(Long userId);
    List<MerchantApplication> findByApplicationStatusOrderByCreateTimeDesc(String status);
    List<MerchantApplication> findAllByOrderByCreateTimeDesc();
}
