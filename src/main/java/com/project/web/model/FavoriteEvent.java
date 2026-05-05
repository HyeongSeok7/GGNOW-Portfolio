package com.project.web.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "favorite_event",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_favorite_event_username_event_id",
            columnNames = {"username", "event_id"}
        )
    }
)
public class FavoriteEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    public FavoriteEvent() {
    }

    public FavoriteEvent(String username, String eventId) {
        this.username = username;
        this.eventId = eventId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}