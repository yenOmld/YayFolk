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
@Table(name = "travel_posts")
public class DiscoverPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_lang", length = 20)
    private String sourceLang;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "audit_status", length = 20)
    private String auditStatus;

    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    @Column(name = "images", columnDefinition = "MEDIUMTEXT")
    private String images;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "visibility", length = 20)
    private String visibility;

    @Column(name = "status", columnDefinition = "tinyint(1) default 1")
    private Integer status;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "collect_count")
    private Integer collectCount;

    @Column(name = "collection_count")
    private Integer collectionCount;

    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createTime = now;
        updateTime = now;
        if (status == null) {
            status = 1;
        }
        if (viewCount == null) {
            viewCount = 0;
        }
        if (collectCount == null) {
            collectCount = 0;
        }
        if (collectionCount == null) {
            collectionCount = 0;
        }
        if (commentCount == null) {
            commentCount = 0;
        }
        if (auditStatus == null) {
            auditStatus = "pending";
        }
        if (visibility == null || visibility.isEmpty()) {
            visibility = "public";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
