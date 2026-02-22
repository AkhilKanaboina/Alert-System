package com.fleetguard.alertsystem.config;

import com.fleetguard.alertsystem.model.Role;
import com.fleetguard.alertsystem.model.User;
import com.fleetguard.alertsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default users on first startup so the UI demo credentials
 * (admin / admin123 and operator / oper123) work out of the box.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createUserIfAbsent("admin", "admin123", Role.ADMIN);
        createUserIfAbsent("operator", "oper123", Role.OPERATOR);
        createUserIfAbsent("user", "user123", Role.USER);
    }

    private void createUserIfAbsent(String username, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .build();
            userRepository.save(user);
            log.info("Seeded default user '{}' with role {}", username, role);
        }
    }
}
