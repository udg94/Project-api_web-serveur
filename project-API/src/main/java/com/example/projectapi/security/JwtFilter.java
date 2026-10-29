package com.example.projectapi.security;

import com.example.projectapi.entities.AuthSession;
import com.example.projectapi.entities.User;
import com.example.projectapi.repository.AuthSessionRepository;
import com.example.projectapi.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;

    public JwtFilter(
            JwtService jwtService,
            UserRepository userRepository,
            AuthSessionRepository authSessionRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");


        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String email = jwtService.getUsername(token);

            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                AuthSession session = authSessionRepository.findById(user.getId()).orElse(null);

                if (session != null && session.getToken().equals(token) && session.getExpiresAt().isAfter(LocalDateTime.now())) {

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null, List.of());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            // token invalide / expiré mettre un message d'erreur
        }

        filterChain.doFilter(request, response);
    }
}
