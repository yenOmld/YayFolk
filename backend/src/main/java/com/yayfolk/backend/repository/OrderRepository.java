package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdAndDeleteStatusOrderByCreateTimeDesc(Long userId, Integer deleteStatus);
    List<Order> findByMerchantIdOrderByCreateTimeDesc(Long merchantId);
    List<Order> findByMerchantIdAndStatusOrderByCreateTimeDesc(Long merchantId, String status);
}
