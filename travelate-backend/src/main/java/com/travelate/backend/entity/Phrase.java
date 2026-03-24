package com.travelate.backend.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "phrases")
public class Phrase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "text", nullable = false, length = 500)
    private String text;
    
    @Column(name = "original_text", length = 500)
    private String originalText;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "create_time", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = new Date();
    }
}