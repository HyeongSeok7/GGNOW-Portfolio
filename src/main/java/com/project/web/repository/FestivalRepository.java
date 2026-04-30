package com.project.web.repository;

import com.project.web.model.FestivalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FestivalRepository extends JpaRepository<FestivalEntity, Long> {

    Optional<FestivalEntity> findByIdentityKey(String identityKey);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO festival (identity_key, normalized_title, title)
            VALUES (:identityKey, :normalizedTitle, :title)
            """, nativeQuery = true)
    void insertIgnore(String identityKey, String normalizedTitle, String title);
}