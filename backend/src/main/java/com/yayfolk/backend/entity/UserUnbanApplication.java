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
@Table(name = "user_unban_applications")
public class UserUnbanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "apply_reason", nullable = false, length = 500)
    private String applyReason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_remark", length = 500)
    private String adminRemark;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "handle_time")
    private Date handleTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (status == null || status.trim().isEmpty()) {
            status = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
