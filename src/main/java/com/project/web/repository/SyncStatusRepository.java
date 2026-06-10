package com.project.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.web.model.SyncStatus;

public interface SyncStatusRepository extends JpaRepository<SyncStatus,Long>{
}
