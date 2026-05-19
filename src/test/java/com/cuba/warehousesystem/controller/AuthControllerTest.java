package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.AuthRequest;
import com.cuba.warehousesystem.dto.AuthResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.UserRole;
import com.cuba.warehousesystem.repository.UserRepository;
import com.cuba.warehousesystem.service.JwtService;
import com.cuba.warehousesystem.service.RevokedTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthController authController;
    private JwtService jwtService;
    private RevokedTokenService revokedTokenService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager", null);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        revokedTokenService = new RevokedTokenService();
        jwtService = new JwtService(revokedTokenService);
        ReflectionTestUtils.setField(jwtService, "secret", "yourVeryLongSecretKeyForDiplomaProject12345678901234567890123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
        userRepository = mock(UserRepository.class);

        authController = new AuthController(authenticationManager, jwtService, revokedTokenService, userRepository);
    }

    @Test
    void loginReturnsJwtToken() {
        AuthResponse response = authController.login(new AuthRequest("manager", "manager123")).getBody();

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(response.token().split("\\.")).hasSize(3);
    }

    @Test
    void meReturnsCurrentUserProfile() {
        User user = new User();
        user.setId(1L);
        user.setUsername("manager");
        user.setFullName("Warehouse Manager");
        user.setEmail("manager@example.com");
        user.setRole(UserRole.MANAGER);
        user.setIsActive(true);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        var response = authController.me(new UsernamePasswordAuthenticationToken("manager", null)).getBody();

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("manager");
        assertThat(response.role()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void logoutRevokesBearerTokenAndRejectsMissingBearerPrefix() {
        String token = jwtService.generateToken("manager");

        var response = authController.logout("Bearer " + token).getBody();

        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("Logged out successfully");
        assertThat(revokedTokenService.isRevoked(token)).isTrue();

        assertThatThrownBy(() -> authController.logout("Basic " + token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Bearer token");
    }
}
