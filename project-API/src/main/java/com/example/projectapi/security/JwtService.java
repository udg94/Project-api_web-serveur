package com.example.projectapi.security;

import com.example.projectapi.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Service
public class JwtService {

    private final SecretKey key =
            Keys.hmacShaKeyFor("courant-parieur-cercueil-climatisation-emballage-agiter-pinceau_7-radom-word" // tmp mettre dans properties après
                    .getBytes(StandardCharsets.UTF_8));

    public String generateToken(User user) {

        return Jwts.builder().subject(user.getEmail()).claim("id", user.getId()).claim("username", user.getUsername()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 1000 * 3600))
                .signWith(key).compact();
    }
    public Long extractUserId(String token) {

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        return ((Number) claims.get("id")).longValue();
    }
    public String getUsername(String token) {

        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();}
}