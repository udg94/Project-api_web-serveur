package com.example.projectapi.entities;
import jakarta.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //id == email

    @Column(unique = true)
    private String email;

    private String username;
    private String mdp;

    public User() {}
    public User(String email, String username, String mdp) {
        this.email = email;
        this.username = username;
        this.mdp = mdp;
    }

    public long getId() {return id;}

    public String getMdp() {return mdp;}

    public void setMdp(String mdp) {this.mdp = mdp;}

    public String getUsername() {return username;}

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
