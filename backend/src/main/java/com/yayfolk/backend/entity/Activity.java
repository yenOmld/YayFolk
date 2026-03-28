package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_profile_id")
    private Long merchantProfileId;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "subtitle", length = 100)
    private String subtitle;

    @Column(name = "cover_image", length = 255)
    private String coverImage;

    @Column(name = "images", columnDefinition = "JSON")
    private String images;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "activity_type", length = 20)
    private String activityType;

    @Column(name = "heritage_type", length = 50)
    private String heritageType;

    @Column(name = "start_time", nullable = false)
    private Date startTime;

    @Column(name = "end_time", nullable = false)
    private Date endTime;

    @Column(name = "signup_start_time")
    private Date signupStartTime;

    @Column(name = "signup_end_time")
    private Date signupEndTime;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants")
    private Integer currentParticipants;

    @Column(name = "price")
    private Integer price;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "location_province", length = 50)
    private String locationProvince;

    @Column(name = "location_city", length = 50)
    private String locationCity;

    @Column(name = "location_district", length = 50)
    private String locationDistrict;

    @Column(name = "location_detail", length = 200)
    private String locationDetail;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "audit_status", length = 20)
    private String auditStatus;

    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    @Column(name = "is_recommend", columnDefinition = "tinyint(1) default 0")
    private Integer isRecommend;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "signup_count")
    private Integer signupCount;

    @Column(name = "collect_count")
    private Integer collectCount;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (status == null) status = "signup";
        if (auditStatus == null) auditStatus = "pending";
        if (currentParticipants == null) currentParticipants = 0;
        if (price == null) price = 0;
        if (viewCount == null) viewCount = 0;
        if (signupCount == null) signupCount = 0;
        if (collectCount == null) collectCount = 0;
        if (isRecommend == null) isRecommend = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
