package com.fleetguard.alertsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetguard.alertsystem.dto.request.RegisterRequest;
import com.fleetguard.alertsystem.dto.request.LoginRequest;
import com.fleetguard.alertsystem.model.User;
import com.fleetguard.alertsystem.repository.UserRepository;
import com.fleetguard.alertsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username '" + req.getUsername() + "' is already taken");
        }
        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole() != null ? req.getRole() : com.fleetguard.alertsystem.model.Role.USER)
                .build();
        userRepository.save(user);
        log.info("Registered new user: {} with role {}", user.getUsername(), user.getRole());
        return Map.of("message", "User registered successfully", "username", user.getUsername(), "role", user.getRole());
    }

    public Map<String, Object> login(LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        log.info("User '{}' logged in", req.getUsername());
        return Map.of(
                "token", token,
                "expiresIn", jwtTokenProvider.getExpirationMs() / 1000,
                "username", user.getUsername(),
                "role", user.getRole()
        );
    }

    public Map<String, Object> getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.fleetguard.alertsystem.exception.ResourceNotFoundException("User", username));
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "createdAt", user.getCreatedAt()
        );
    }
}
