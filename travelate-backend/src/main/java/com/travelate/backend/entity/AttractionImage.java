package com.travelate.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "attraction_images")
public class AttractionImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "attraction_id", nullable = false)
    private Integer attractionId;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "create_time")
    private Date createTime;
}