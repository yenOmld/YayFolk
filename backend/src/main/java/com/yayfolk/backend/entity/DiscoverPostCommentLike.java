package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "travel_post_comment_likes")
public class DiscoverPostCommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
    }
}
