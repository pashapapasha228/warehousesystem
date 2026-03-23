package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.AuthRequest;
import com.cuba.warehousesystem.dto.AuthResponse;
import com.cuba.warehousesystem.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // Аутентификация пользователя
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        // Устанавливаем аутентификацию в контексте безопасности
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Генерируем JWT токен для аутентифицированного пользователя
        String jwt = jwtService.generateToken(authentication.getName());
        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}
