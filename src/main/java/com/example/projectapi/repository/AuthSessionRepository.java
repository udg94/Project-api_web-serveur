package com.example.projectapi.repository;

import com.example.projectapi.entities.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository
        extends JpaRepository<AuthSession, Long> {
}