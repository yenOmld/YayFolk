package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "activity_bookings")
public class ActivityBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "signup_time")
    private Date signupTime;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "payment_time")
    private Date paymentTime;

    @Column(name = "participant_name", length = 50)
    private String participantName;

    @Column(name = "participant_phone", length = 20)
    private String participantPhone;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "qr_code", length = 100)
    private String qrCode;

    @Column(name = "verification_time")
    private Date verificationTime;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        signupTime = new Date();
        if (status == null) status = "registered";
        if (paymentStatus == null) paymentStatus = "unpaid";
        if (participantCount == null) participantCount = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
