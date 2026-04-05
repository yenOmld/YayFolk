package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "official_contents")
public class OfficialContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "cover_image", length = 255)
    private String coverImage;

    @Column(name = "images", columnDefinition = "JSON")
    private String images;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Column(name = "is_public", columnDefinition = "tinyint(1) default 1")
    private Integer isPublic;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (isPublic == null) isPublic = 1;
        if (viewCount == null) viewCount = 0;
        if (category == null) category = "introduction";
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
