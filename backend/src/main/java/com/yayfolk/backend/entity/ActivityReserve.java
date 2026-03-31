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
@Table(name = "activity_reserves")
public class ActivityReserve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserve_no", nullable = false, length = 64)
    private String reserveNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "activity_title", nullable = false, length = 128)
    private String activityTitle;

    @Column(name = "activity_time", nullable = false, length = 128)
    private String activityTime;

    @Column(name = "contact_name", nullable = false, length = 32)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "participant_num", nullable = false)
    private Integer participantNum;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "pay_amount", nullable = false)
    private Integer payAmount;

    @Column(name = "pay_status")
    private Integer payStatus;

    @Column(name = "reserve_status", length = 32)
    private String reserveStatus;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "payment_type", length = 32)
    private String paymentType;

    @Column(name = "payment_time")
    private Date paymentTime;

    @Column(name = "cancel_time")
    private Date cancelTime;

    @Column(name = "verify_time")
    private Date verifyTime;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createTime == null) {
            createTime = now;
        }
        updateTime = now;
        if (participantNum == null || participantNum <= 0) {
            participantNum = 1;
        }
        if (payStatus == null) {
            payStatus = 0;
        }
        if (reserveStatus == null || reserveStatus.trim().isEmpty()) {
            reserveStatus = "registered";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
