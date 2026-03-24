package com.travelate.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type", nullable = false, length = 20)
    private String type = "chat";

    @Column(name = "user1_id")
    private Long user1Id;

    @Column(name = "user2_id")
    private Long user2Id;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_message_time")
    private Date lastMessageTime;

    @Column(name = "unread_count_user1")
    private Integer unreadCountUser1 = 0;

    @Column(name = "unread_count_user2")
    private Integer unreadCountUser2 = 0;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
        if (unreadCountUser1 == null) {
            unreadCountUser1 = 0;
        }
        if (unreadCountUser2 == null) {
            unreadCountUser2 = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
