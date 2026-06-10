package com.project.web.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SyncStatus {

    @Id
    private Long id;

    private LocalDateTime lastSuccessTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getLastSuccessTime() {
        return lastSuccessTime;
    }

    public void setLastSuccessTime(LocalDateTime lastSuccessTime) {
        this.lastSuccessTime = lastSuccessTime;
    }
}