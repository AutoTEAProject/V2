package com.autotea.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "google_sub", nullable = false, unique = true, length = 255)
    private String googleSub;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "picture_url", length = 1000)
    private String pictureUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt = Instant.now();

    public User(String email, String googleSub, String displayName, String pictureUrl) {
        this.email = email;
        this.googleSub = googleSub;
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
    }
}
