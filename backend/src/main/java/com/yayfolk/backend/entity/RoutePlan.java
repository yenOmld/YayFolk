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
@Table(name = "route_plans")
public class RoutePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(name = "matched_count")
    private Integer matchedCount;

    @Column(name = "suggested_days")
    private Integer suggestedDays;

    @Column(name = "route_text", columnDefinition = "LONGTEXT")
    private String routeText;

    @Column(name = "daily_plans", columnDefinition = "JSON")
    private String dailyPlans;

    @Column(name = "budget", columnDefinition = "JSON")
    private String budget;

    @Column(name = "travel_tips", columnDefinition = "JSON")
    private String travelTips;

    @Column(name = "parsed_query", columnDefinition = "JSON")
    private String parsedQuery;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
