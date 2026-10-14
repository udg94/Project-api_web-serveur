package com.example.projectapi.entities;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class AuthSession {

    @Id
    private Long userId;

    private String token;
    private LocalDateTime expiresAt;

    public AuthSession(Long userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public AuthSession() {

    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
