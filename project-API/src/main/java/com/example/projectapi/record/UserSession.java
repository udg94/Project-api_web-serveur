package com.example.projectapi.record;

import java.time.LocalDateTime;

public record UserSession(Long id, String username, String email, LocalDateTime expiresAt) { }
