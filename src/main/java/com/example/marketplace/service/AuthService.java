package com.example.marketplace.service;

import com.example.marketplace.api.dto.AuthResponse;
import com.example.marketplace.api.dto.CreateUserRequest;
import com.example.marketplace.api.dto.LoginRequest;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(CreateUserRequest request) {
        User user = userService.create(request);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userService.findByEmail(request.email());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getNome(),
                user.getEmail(),
                userService.maskWhatsapp(user.getWhatsapp()),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}
