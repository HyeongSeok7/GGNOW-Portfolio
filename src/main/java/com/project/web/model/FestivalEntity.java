package com.project.web.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "festival",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_festival_identity_key",
                columnNames = "identity_key"
        )
)
public class FestivalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제목 + 날짜 + 시간 + 장소 + 참가비 등을 조합한 내부 식별 키
    @Column(name = "identity_key", nullable = false, length = 64)
    private String identityKey;

    @Column(name = "normalized_title", nullable = false)
    private String normalizedTitle;

    @Column(nullable = false)
    private String title;

    public Long getId() {
        return id;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public void setNormalizedTitle(String normalizedTitle) {
        this.normalizedTitle = normalizedTitle;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}