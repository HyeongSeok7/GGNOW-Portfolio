package com.project.web.repository;

import com.project.web.model.FavoriteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FavoriteEventRepository extends JpaRepository<FavoriteEvent, Long> {

    List<FavoriteEvent> findByUsername(String username);

    List<FavoriteEvent> findAllByUsername(String username);

    @Transactional
    @Modifying
    void deleteByUsernameAndEventId(String username, String eventId);

    boolean existsByUsernameAndEventId(String username, String eventId);
}