package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "type", length = 20)
    private String type = "text";

    @Column(name = "source_lang", length = 10)
    private String sourceLang;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;

    @Column(name = "deleted_by_receiver")
    private Boolean deletedByReceiver = false;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (isRead == null) {
            isRead = false;
        }
        if (type == null) {
            type = "text";
        }
        if (deletedBySender == null) {
            deletedBySender = false;
        }
        if (deletedByReceiver == null) {
            deletedByReceiver = false;
        }
    }
}
