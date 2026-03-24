package com.travelate.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "attractions")
public class Attraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro;

    @Column(name = "story", columnDefinition = "TEXT")
    private String story;

    @Column(name = "lng", precision = 10, scale = 6, nullable = false)
    private BigDecimal lng;

    @Column(name = "lat", precision = 10, scale = 6, nullable = false)
    private BigDecimal lat;

    @Column(name = "radius", nullable = false)
    private Integer radius = 150;

    @Column(name = "cover", length = 255)
    private String cover;

    @Column(name = "rating", length = 20)
    private String rating;

    @Column(name = "duration", length = 50)
    private String duration;

    @Column(name = "best_season", length = 50)
    private String bestSeason;

    @Column(name = "create_time", updatable = false)
    private Date createTime;
}
