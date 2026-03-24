package com.travelate.backend.repository;

import com.travelate.backend.entity.AttractionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttractionImageRepository extends JpaRepository<AttractionImage, Integer> {
    List<AttractionImage> findByAttractionIdOrderByOrderIndexAsc(Integer attractionId);
}