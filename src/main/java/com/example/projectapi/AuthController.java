package com.example.projectapi;

import com.example.projectapi.entities.AuthResponse;
import com.example.projectapi.entities.User;
import com.example.projectapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        if (save_user.getMdp().equals(passwordEncoder.encode(user.getMdp()))){
            String token = jwtService.generateToken(save_user.getEmail());

            return new AuthResponse(save_user.getId(), save_user.getUsername(), save_user.getEmail(), token);
        }

        throw new IllegalArgumentException("wrong password");


    }
}
