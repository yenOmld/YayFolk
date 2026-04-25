package com.yayfolk.backend.entity;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "explore_messages")
public class ExploreMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "intent", length = 30)
    private String intent;

    @Column(name = "resources_json", columnDefinition = "TEXT")
    private String resourcesJson;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
    }
}
