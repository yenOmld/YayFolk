package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
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

    @Column(name = "shop_intro", length = 500)
    private String shopIntro;

    @Column(name = "status", columnDefinition = "tinyint(1) default 1")
    private Integer status;

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
        if (followerCount == null) followerCount = 0;
        if (followingCount == null) followingCount = 0;
        if (isSuperAdmin == null) isSuperAdmin = 0;
        if (country == null || country.isEmpty()) {
            country = "China";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
