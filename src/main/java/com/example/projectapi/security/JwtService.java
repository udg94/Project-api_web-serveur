package com.example.projectapi.security;

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

    public String generateToken(String username) {

        return Jwts.builder().subject(username).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 1000 * 3600))
                .signWith(key).compact();
    }

    public String getUsername(String token) {

        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}