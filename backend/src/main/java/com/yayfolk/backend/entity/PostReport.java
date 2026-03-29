package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "post_reports")
public class PostReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handle_time")
    private Date handleTime;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        if (status == null || status.trim().isEmpty()) {
            status = "pending";
        }
        createTime = new Date();
    }
}
