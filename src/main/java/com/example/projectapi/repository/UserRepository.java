package com.example.projectapi.repository;

import com.example.projectapi.entities.User;

import com.example.projectapi.record.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String username);
    Optional<User> findById(Long id);

    @Query("""
        SELECT u.id, u.username, u.email, s.expiresAt
        FROM User u
        LEFT JOIN AuthSession s ON s.userId = u.id
        ORDER BY u.username
    """)
    List<UserSession> findAllUsersWithSessionExpiration();
}


