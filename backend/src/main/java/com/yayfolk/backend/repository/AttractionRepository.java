package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Integer> {
    Optional<Attraction> findByName(String name);
    
    @Query(value = "SELECT *, " +
            "(6371000 * acos(cos(radians(:lat)) * cos(radians(lat)) * " +
            "cos(radians(lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(lat)))) AS distance " +
            "FROM attractions " +
            "WHERE (6371000 * acos(cos(radians(:lat)) * cos(radians(lat)) * " +
            "cos(radians(lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(lat)))) < :radius " +
            "ORDER BY distance ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Attraction> findNearbyAttractions(@Param("lng") double lng, @Param("lat") double lat, 
                                           @Param("radius") double radius, @Param("limit") int limit);

    @Query(value = "SELECT *, " +
            "(6371000 * acos(cos(radians(:lat)) * cos(radians(lat)) * " +
            "cos(radians(lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(lat)))) AS distance " +
            "FROM attractions " +
            "WHERE name = :name AND " +
            "(6371000 * acos(cos(radians(:lat)) * cos(radians(lat)) * " +
            "cos(radians(lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(lat)))) < :radius " +
            "ORDER BY distance ASC " +
            "LIMIT 1", nativeQuery = true)
    Optional<Attraction> findByNameWithinRadius(@Param("lng") double lng, @Param("lat") double lat,
                                                @Param("radius") double radius, @Param("name") String name);
}
