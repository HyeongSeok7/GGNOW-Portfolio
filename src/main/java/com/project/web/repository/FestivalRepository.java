package com.project.web.repository;

import com.project.web.model.FestivalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FestivalRepository extends JpaRepository<FestivalEntity, Long> {

    Optional<FestivalEntity> findByIdentityKey(String identityKey);
}