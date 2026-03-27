package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByMerchantIdOrderByCreateTimeDesc(Long merchantId);
    List<Product> findByStatusOrderByCreateTimeDesc(String status);
    List<Product> findByMerchantIdAndStatusOrderByCreateTimeDesc(Long merchantId, String status);
}
