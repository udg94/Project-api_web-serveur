package com.example.projectapi;

import com.example.projectapi.record.AuthResponse;
import com.example.projectapi.entities.AuthSession;
import com.example.projectapi.entities.User;
import com.example.projectapi.record.UserActivity;
import com.example.projectapi.record.UserSession;
import com.example.projectapi.repository.AuthSessionRepository;
import com.example.projectapi.repository.UserRepository;
import com.example.projectapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class APPController {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthSessionRepository authSessionRepository;

    public APPController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthSessionRepository authSessionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authSessionRepository = authSessionRepository;
    }

    @PostMapping("/comptes")
    public AuthResponse addUser(@RequestBody User user) {
        if(user.getEmail() == null || user.getEmail().isEmpty()){
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getMdp() == null  || user.getMdp().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if(user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        User new_user = new User();

        new_user.setEmail(user.getEmail());
        new_user.setUsername(user.getUsername());
        new_user.setMdp(passwordEncoder.encode(user.getMdp()));
        new_user = userRepository.save(new_user);

        String token = jwtService.generateToken(new_user.getEmail());
        AuthSession session = new AuthSession(new_user.getId(), token, LocalDateTime.now().plusHours(1));

        authSessionRepository.save(session);
        return new AuthResponse(new_user.getId(), new_user.getUsername(), new_user.getEmail(), token);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@RequestBody User user) {
        if(user.getEmail() == null || user.getEmail().isEmpty()){
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getMdp() == null  || user.getMdp().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        Optional<User> is_user = userRepository.findByEmail(user.getEmail());
        if (is_user.isEmpty()) {throw new IllegalArgumentException("user not found");}

        User save_user = is_user.get();
        if (passwordEncoder.matches(user.getMdp(), save_user.getMdp())){
            String token = jwtService.generateToken(save_user.getEmail());

            AuthSession session = new AuthSession(save_user.getId(), token, LocalDateTime.now().plusHours(1));

            authSessionRepository.save(session);
            return new AuthResponse(save_user.getId(), save_user.getUsername(), save_user.getEmail(), token);
        }

        throw new IllegalArgumentException("wrong password");


    }

    @GetMapping("/utilisateurs")
    public List<UserActivity> get_user_activity(){
        List<UserActivity> userActivity = userRepository.findAllUsersWithSessionExpiration().stream().map(userSession ->
                new UserActivity( userSession.email(),
                        userSession.expiresAt() != null && userSession.expiresAt().isAfter(LocalDateTime.now())
                )
        ).toList();
        return userActivity;
    }
}
