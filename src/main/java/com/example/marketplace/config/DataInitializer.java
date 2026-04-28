package com.example.marketplace.config;

import com.example.marketplace.domain.enums.UserRole;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.UserRepository;
import com.example.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@marketplace.com").orElseGet(() -> {
            User admin = new User();
            admin.setNome("Administrador");
            admin.setEmail("admin@marketplace.com");
            admin.setWhatsapp("11999999999");
            admin.setAvatarUrl(userService.buildDefaultAvatarUrl("Administrador"));
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            return userRepository.save(admin);
        });
    }
}
