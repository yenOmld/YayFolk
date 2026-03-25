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

    @Column(name = "status", columnDefinition = "tinyint(1) default 1")
    private Integer status;

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
        if (country == null || country.isEmpty()) {
            country = "China";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
