package com.project.web.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(name = "festival_id", nullable = false)
    private Long festivalId;

    @Column(nullable = false)
    private String festivalTitle; // 스냅샷(조회/표시용)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getContent() { return content; }
    public String getUsername() { return username; }
    public Long getFestivalId() { return festivalId; }
    public String getFestivalTitle() { return festivalTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setContent(String content) { this.content = content; }
    public void setUsername(String username) { this.username = username; }
    public void setFestivalId(Long festivalId) { this.festivalId = festivalId; }
    public void setFestivalTitle(String festivalTitle) { this.festivalTitle = festivalTitle; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}