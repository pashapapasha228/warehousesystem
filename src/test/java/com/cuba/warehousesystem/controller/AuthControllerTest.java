package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.AuthRequest;
import com.cuba.warehousesystem.dto.AuthResponse;
import com.cuba.warehousesystem.repository.UserRepository;
import com.cuba.warehousesystem.service.JwtService;
import com.cuba.warehousesystem.service.RevokedTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthController authController;

    @BeforeEach
    void setUp() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager", null);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        JwtService jwtService = new JwtService(new RevokedTokenService());
        ReflectionTestUtils.setField(jwtService, "secret", "yourVeryLongSecretKeyForDiplomaProject12345678901234567890123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        authController = new AuthController(authenticationManager, jwtService, new RevokedTokenService(), mock(UserRepository.class));
    }

    @Test
    void loginReturnsJwtToken() {
        AuthResponse response = authController.login(new AuthRequest("manager", "manager123")).getBody();

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(response.token().split("\\.")).hasSize(3);
    }
}
