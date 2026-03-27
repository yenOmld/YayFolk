package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "merchant_applications")
public class MerchantApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "id_card", length = 18)
    private String idCard;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "heritage_type", length = 50)
    private String heritageType;

    @Column(name = "heritage_description", columnDefinition = "TEXT")
    private String heritageDescription;

    @Column(name = "proof_images", columnDefinition = "JSON")
    private String proofImages;

    @Column(name = "shop_name", length = 100)
    private String shopName;

    @Column(name = "shop_address", length = 200)
    private String shopAddress;

    @Column(name = "application_status", length = 20)
    private String applicationStatus;

    @Column(name = "audit_admin_id")
    private Long auditAdminId;

    @Column(name = "audit_time")
    private Date auditTime;

    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (applicationStatus == null) applicationStatus = "pending";
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
