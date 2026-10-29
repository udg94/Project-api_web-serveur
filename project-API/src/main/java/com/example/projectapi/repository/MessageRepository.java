package com.example.projectapi.repository;

import com.example.projectapi.entities.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer>{

    @Query("""
        SELECT m
        FROM  Message as m
        WHERE (m.expediteur_id = :me_id AND m.destination_id = :is_id)
            OR (m.destination_id = :me_id AND m.expediteur_id = :is_id)
        ORDER BY m.date ASC
    """)
    List<Message> getAllMessage(@Param("me_id") Long meId, @Param("is_id") Long isId);

}


