package com.travelate.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "travel_post_history")
public class DiscoverPostHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "last_view_time")
    private Date lastViewTime;

    @PrePersist
    protected void onCreate() {
        if (viewCount == null) {
            viewCount = 1;
        }
        if (lastViewTime == null) {
            lastViewTime = new Date();
        }
    }
}
