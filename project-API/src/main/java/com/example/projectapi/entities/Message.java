package com.example.projectapi.entities;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long expediteur_id;
    private Long destination_id;
    private String expediteur_username;
    private String contenu;
    private LocalDateTime date;

    public Message() {}
    public Message(Long expediteur_id, Long destination_id, String expediteur_username, String contenu, LocalDateTime date){
        this.contenu = contenu;
        this.date = date;
        this.expediteur_id = expediteur_id;
        this.destination_id = destination_id;
        this.expediteur_username = expediteur_username;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExpediteur_id() {
        return expediteur_id;
    }

    public void setExpediteur_id(Long expediteur_id) {
        this.expediteur_id = expediteur_id;
    }

    public Long getDestination_id() {
        return destination_id;
    }

    public void setDestination_id(Long destination_id) {
        this.destination_id = destination_id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
