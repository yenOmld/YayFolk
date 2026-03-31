package com.yayfolk.backend.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.util.Date;

@Data
@Entity
@Table(name = "reserve_status_logs")
public class ReserveStatusLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserve_id", nullable = false)
    private Long reserveId;

    @Column(name = "old_status", length = 32)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_type", nullable = false, length = 16)
    private String operatorType;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "create_time")
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = new Date();
        }
    }
}
