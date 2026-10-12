package com.example.projectapi.repository;

import com.example.projectapi.entities.Message;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Integer>{
}


