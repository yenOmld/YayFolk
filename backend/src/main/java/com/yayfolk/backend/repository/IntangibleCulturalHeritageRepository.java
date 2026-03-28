package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntangibleCulturalHeritageRepository extends JpaRepository<IntangibleCulturalHeritage, Long> {
    List<IntangibleCulturalHeritage> findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
}
