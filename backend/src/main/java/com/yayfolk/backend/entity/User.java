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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "github_id", unique = true, length = 100)
    private String githubId;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "lang_code", length = 10)
    private String langCode;

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar", columnDefinition = "MEDIUMTEXT")
    private String avatar;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "bio", length = 200)
    private String bio;

    @Column(name = "signature", length = 120)
    private String signature;

    @Column(name = "follower_count")
    private Integer followerCount;

    @Column(name = "following_count")
    private Integer followingCount;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "shop_status", length = 20)
    private String shopStatus;

    @Column(name = "shop_name", length = 100)
    private String shopName;

    @Column(name = "shop_cover", length = 255)
    private String shopCover;

    @Column(name = "cover_photo", length = 255)
    private String coverPhoto;

    @Column(name = "shop_intro", length = 500)
    private String shopIntro;

    @Column(name = "collection_visibility", length = 20)
    private String collectionVisibility;

    @Column(name = "is_merchant", columnDefinition = "tinyint(1) default 0")
    private Integer isMerchant;

    @Column(name = "status", columnDefinition = "tinyint(1) default 1")
    private Integer status;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    @Column(name = "ban_admin_id")
    private Long banAdminId;

    @Column(name = "ban_time")
    private Date banTime;

    @Column(name = "is_super_admin", columnDefinition = "tinyint(1) default 0")
    private Integer isSuperAdmin;

    @Column(name = "last_login_time")
    private Date lastLoginTime;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        status = 1;
        if (role == null) role = "user";
        if (shopStatus == null) shopStatus = "none";
        if (collectionVisibility == null || collectionVisibility.isEmpty()) collectionVisibility = "public";
        if (followerCount == null) followerCount = 0;
        if (followingCount == null) followingCount = 0;
        if (isSuperAdmin == null) isSuperAdmin = 0;
        if (isMerchant == null) isMerchant = 0;
        if (country == null || country.isEmpty()) {
            country = "China";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
