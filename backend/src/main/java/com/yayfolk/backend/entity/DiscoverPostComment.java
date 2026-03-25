package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "travel_post_comments")
public class DiscoverPostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "reply_to_user_id")
    private Long replyToUserId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "source_lang", length = 20)
    private String sourceLang;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (likeCount == null) {
            likeCount = 0;
        }
    }
}
