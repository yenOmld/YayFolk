package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, nullable = false, length = 50)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_profile_id")
    private Long merchantProfileId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "pay_amount", nullable = false)
    private Integer payAmount;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "payment_type", length = 20)
    private String paymentType;

    @Column(name = "payment_time")
    private Date paymentTime;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "receiver_province", length = 50)
    private String receiverProvince;

    @Column(name = "receiver_city", length = 50)
    private String receiverCity;

    @Column(name = "receiver_district", length = 50)
    private String receiverDistrict;

    @Column(name = "receiver_address", length = 200)
    private String receiverAddress;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "logistics_company", length = 50)
    private String logisticsCompany;

    @Column(name = "logistics_no", length = 50)
    private String logisticsNo;

    @Column(name = "delete_status", columnDefinition = "tinyint(1) default 0")
    private Integer deleteStatus;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (status == null) status = "pending_payment";
        if (deleteStatus == null) deleteStatus = 0;
        if (quantity == null) quantity = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
