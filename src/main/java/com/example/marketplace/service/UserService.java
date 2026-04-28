package com.example.marketplace.service;

import com.example.marketplace.api.dto.CreateUserRequest;
import com.example.marketplace.domain.enums.UserRole;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.UserRepository;
import com.example.marketplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User create(CreateUserRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            throw new BusinessException("E-mail já cadastrado");
        });

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setWhatsapp(request.whatsapp());
        user.setAvatarUrl(buildDefaultAvatarUrl(request.nome()));
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(buildDefaultAvatarUrl(user.getNome()));
            user = userRepository.save(user);
        }

        return user;
    }

    public Long findUserIdByEmail(String email) {
        return findByEmail(email).getId();
    }

    public String maskWhatsapp(String whatsapp) {
        if (whatsapp == null || whatsapp.length() < 4) return "****";
        String visible = whatsapp.substring(whatsapp.length() - 4);
        return "***" + visible;
    }

    public String sanitizeWhatsapp(String whatsapp) {
        return whatsapp.replaceAll("\\D", "");
    }

    public String buildDefaultAvatarUrl(String nome) {
        String safeName = URLEncoder.encode(nome == null || nome.isBlank() ? "Usuario" : nome, StandardCharsets.UTF_8);
        return "https://ui-avatars.com/api/?name=" + safeName + "&background=1f6feb&color=ffffff&size=256";
    }
}
