package com.yayfolk.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "subtitle", length = 100)
    private String subtitle;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "sales")
    private Integer sales;

    @Column(name = "main_image", length = 255)
    private String mainImage;

    @Column(name = "images", columnDefinition = "JSON")
    private String images;

    @Column(name = "heritage_type", length = 50)
    private String heritageType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "is_recommend", columnDefinition = "tinyint(1) default 0")
    private Integer isRecommend;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "sales_count")
    private Integer salesCount;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        if (status == null) status = "on_sale";
        if (stock == null) stock = 0;
        if (sales == null) sales = 0;
        if (viewCount == null) viewCount = 0;
        if (salesCount == null) salesCount = 0;
        if (isRecommend == null) isRecommend = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
