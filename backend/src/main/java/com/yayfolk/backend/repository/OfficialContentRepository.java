package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.OfficialContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficialContentRepository extends JpaRepository<OfficialContent, Long> {
    List<OfficialContent> findByIsPublicOrderByCreateTimeDesc(Integer isPublic);
    List<OfficialContent> findAllByOrderByCreateTimeDesc();
    List<OfficialContent> findByIsPublicAndCategoryOrderByCreateTimeDesc(Integer isPublic, String category);
    OfficialContent findTopByCategoryAndIsPublicOrderByCreateTimeDesc(String category, Integer isPublic);
    OfficialContent findByActivityId(Long activityId);
}
