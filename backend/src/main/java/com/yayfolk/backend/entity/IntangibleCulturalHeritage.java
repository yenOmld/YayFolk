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
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "intangible_cultural_heritage")
public class IntangibleCulturalHeritage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "subcategory", length = 50)
    private String subcategory;

    @Column(name = "dynasty", length = 50)
    private String dynasty;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "history", columnDefinition = "TEXT")
    private String history;

    @Column(name = "inheritance_value", columnDefinition = "TEXT")
    private String inheritanceValue;

    @Column(name = "representative_inheritor", length = 100)
    private String representativeInheritor;

    @Column(name = "images", columnDefinition = "JSON")
    private String images;

    @Column(name = "video_url", length = 255)
    private String videoUrl;

    @Column(name = "related_poems", columnDefinition = "JSON")
    private String relatedPoems;

    @Column(name = "related_solar_terms", columnDefinition = "JSON")
    private String relatedSolarTerms;

    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_featured", columnDefinition = "tinyint(1) default 0")
    private Integer isFeatured;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (isFeatured == null) {
            isFeatured = 0;
        }
        if (viewCount == null) {
            viewCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
