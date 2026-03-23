package com.project.web.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "festival",
        uniqueConstraints = @UniqueConstraint(columnNames = "normalized_title")
)
public class FestivalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="normalized_title", nullable = false)
    private String normalizedTitle;

    @Column(nullable = false)
    private String title;

    public Long getId() { return id; }
    public String getNormalizedTitle() { return normalizedTitle; }
    public String getTitle() { return title; }

    public void setId(Long id) { this.id = id; }
    public void setNormalizedTitle(String normalizedTitle) { this.normalizedTitle = normalizedTitle; }
    public void setTitle(String title) { this.title = title; }
}