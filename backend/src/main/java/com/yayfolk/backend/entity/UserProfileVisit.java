package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(
    name = "user_profile_visits",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_profile_visit_pair", columnNames = {"viewer_id", "profile_user_id"})
    }
)
public class UserProfileVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @Column(name = "profile_user_id", nullable = false)
    private Long profileUserId;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createTime = now;
        updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
