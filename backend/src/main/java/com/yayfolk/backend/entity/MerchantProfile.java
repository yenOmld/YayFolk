package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.util.Date;

@Data
@Entity
@Table(name = "merchant_profiles")
public class MerchantProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "shop_name", length = 100)
    private String shopName;

    @Column(name = "shop_cover", length = 255)
    private String shopCover;

    @Column(name = "shop_intro", length = 500)
    private String shopIntro;

    @Column(name = "contact_name", length = 50)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "province", length = 32)
    private String province;

    @Column(name = "city", length = 32)
    private String city;

    @Column(name = "district", length = 32)
    private String district;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "heritage_type", length = 50)
    private String heritageType;

    @Column(name = "heritage_description", columnDefinition = "TEXT")
    private String heritageDescription;

    @Column(name = "proof_images", columnDefinition = "JSON")
    private String proofImages;

    @Column(name = "business_status", length = 20, nullable = false)
    private String businessStatus;

    @Column(name = "latest_application_id")
    private Long latestApplicationId;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (businessStatus == null) {
            businessStatus = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
