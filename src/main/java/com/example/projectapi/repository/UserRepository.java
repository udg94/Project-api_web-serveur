package com.example.projectapi.repository;

import com.example.projectapi.entities.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String username);
    Optional<User> findById(Long id);
}


