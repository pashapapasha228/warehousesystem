package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.config.RestAccessDeniedHandler;
import com.cuba.warehousesystem.config.RestAuthenticationEntryPoint;
import com.cuba.warehousesystem.event.EdiMessageProcessedEvent;
import com.cuba.warehousesystem.event.EdiMessageReceivedEvent;
import com.cuba.warehousesystem.event.OperationCompletedEvent;
import com.cuba.warehousesystem.event.OperationCreatedEvent;
import com.cuba.warehousesystem.event.StockBalanceChangedEvent;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.exception.GlobalExceptionHandler;
import com.cuba.warehousesystem.listener.AuditEventListener;
import com.cuba.warehousesystem.listener.EdiProcessingListener;
import com.cuba.warehousesystem.listener.ReportCacheEvictListener;
import com.cuba.warehousesystem.model.AuditLog;
import com.cuba.warehousesystem.model.EdiAuditLog;
import com.cuba.warehousesystem.model.EdiAuditStatus;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.UserRole;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import com.cuba.warehousesystem.repository.EdiAuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfrastructureServicesTest {

    @Test
    void revokedTokenServiceDropsExpiredTokensAndKeepsActiveTokens() {
        RevokedTokenService service = new RevokedTokenService();

        service.revoke("expired", new Date(System.currentTimeMillis() - 1000));
        service.revoke("active", new Date(System.currentTimeMillis() + 60_000));

        assertThat(service.isRevoked("expired")).isFalse();
        assertThat(service.isRevoked("active")).isTrue();
        assertThat(service.isRevoked("missing")).isFalse();
    }

    @Test
    void jwtServiceRejectsRevokedToken() {
        RevokedTokenService revokedTokenService = new RevokedTokenService();
        JwtService jwtService = new JwtService(revokedTokenService);
        ReflectionTestUtils.setField(jwtService, "secret", "yourVeryLongSecretKeyForDiplomaProject12345678901234567890123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtService, "expiration", 86_400_000L);
        String token = jwtService.generateToken("manager");
        revokedTokenService.revoke(token, jwtService.extractExpiration(token));

        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername("manager")
                .password("hash")
                .authorities("ROLE_MANAGER")
                .build();

        assertThat(jwtService.extractUsername(token)).isEqualTo("manager");
        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void userDetailsServiceLoadsActiveUserWithRoleAuthorityAndRejectsDisabledUsers() {
        UserRepository repository = mock(UserRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(repository);
        User active = user("manager", true);
        when(repository.findByUsername("manager")).thenReturn(Optional.of(active));

        var details = service.loadUserByUsername("manager");

        assertThat(details.getUsername()).isEqualTo("manager");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_MANAGER");

        when(repository.findByUsername("disabled")).thenReturn(Optional.of(user("disabled", false)));
        assertThatThrownBy(() -> service.loadUserByUsername("disabled"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void auditLogServiceUsesSystemUsernameWhenBlank() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository);

        service.write("Product", "1", "UPDATED", " ", "{\"ok\":true}");

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(log ->
                "Product".equals(log.getEntityName())
                        && "system".equals(log.getUsername())
                        && log.getOccurredAt() != null
        ));
    }

    @Test
    void globalExceptionHandlerMapsCommonExceptionFamilies() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        assertThat(handler.handleNotFound(new EntityNotFoundException("missing"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleBadRequest(new BadRequestException("bad"), request).getBody().message())
                .isEqualTo("bad");
        assertThat(handler.handleBadRequest(mock(HttpMessageNotReadableException.class), request).getBody().message())
                .isEqualTo("Malformed request body");
        assertThat(handler.handleBadCredentials(new BadCredentialsException("no"), request).getBody().message())
                .isEqualTo("Invalid username or password");
        assertThat(handler.handleAuthentication(mock(AuthenticationException.class), request).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleAccessDenied(new AccessDeniedException("denied"), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PATCH"), request).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(handler.handleUnexpected(new RuntimeException("boom"), request).getBody().message())
                .isEqualTo("Unexpected server error");
    }

    @Test
    void restSecurityHandlersWriteJsonErrors() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");

        MockHttpServletResponse unauthorized = new MockHttpServletResponse();
        new RestAuthenticationEntryPoint(objectMapper).commence(request, unauthorized, mock(AuthenticationException.class));
        assertThat(unauthorized.getStatus()).isEqualTo(401);
        assertThat(unauthorized.getContentAsString()).contains("Authentication is required");

        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        new RestAccessDeniedHandler(objectMapper).handle(request, forbidden, new AccessDeniedException("denied"));
        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getContentAsString()).contains("Access denied");
    }

    @Test
    void auditEventListenerWritesExpectedAuditEntries() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditEventListener listener = new AuditEventListener(auditLogService);
        LocalDateTime now = LocalDateTime.now();

        listener.onOperationCreated(new OperationCreatedEvent(1L, "INCOME-1", OperationType.INCOME, 10L, "manager", now));
        listener.onOperationCompleted(new OperationCompletedEvent(1L, "INCOME-1", OperationType.INCOME, 10L, "manager", now));
        listener.onStockBalanceChanged(new StockBalanceChangedEvent(1L, 2L, 3L, 4, 5, OperationType.MOVE, "storekeeper", now));
        listener.onEdiMessageReceived(new EdiMessageReceivedEvent(6L, EdiMessageType.DESADV, 7L, "MSG-1", now));
        listener.onEdiMessageProcessed(new EdiMessageProcessedEvent(6L, EdiMessageType.DESADV, EdiMessageStatus.PROCESSED, 1L, null, now));

        verify(auditLogService).write(eq("Operation"), eq("1"), eq("CREATED"), eq("manager"), org.mockito.ArgumentMatchers.contains("INCOME-1"));
        verify(auditLogService).write(eq("Operation"), eq("1"), eq("COMPLETED"), eq("manager"), org.mockito.ArgumentMatchers.contains("INCOME-1"));
        verify(auditLogService).write(eq("StockBalance"), eq("2:3"), eq("CHANGED"), eq("storekeeper"), org.mockito.ArgumentMatchers.contains("previousQuantity"));
        verify(auditLogService).write(eq("EdiMessage"), eq("6"), eq("RECEIVED"), eq("system"), org.mockito.ArgumentMatchers.contains("MSG-1"));
        verify(auditLogService).write(eq("EdiMessage"), eq("6"), eq("PROCESSED"), eq("system"), org.mockito.ArgumentMatchers.contains("PROCESSED"));
    }

    @Test
    void ediProcessingListenerQueuesReceivedMessageOnceAndAuditsProcessingOutcome() {
        EdiMessageRepository messageRepository = mock(EdiMessageRepository.class);
        EdiProcessingQueueRepository queueRepository = mock(EdiProcessingQueueRepository.class);
        EdiAuditLogRepository auditRepository = mock(EdiAuditLogRepository.class);
        EdiProcessingListener listener = new EdiProcessingListener(messageRepository, queueRepository, auditRepository);
        EdiMessage message = new EdiMessage();
        message.setId(10L);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(queueRepository.existsByEdiMessage_Id(10L)).thenReturn(false);

        listener.onEdiMessageReceived(new EdiMessageReceivedEvent(10L, EdiMessageType.ORDERS, 1L, "MSG-10", LocalDateTime.now()));

        verify(queueRepository).save(any(EdiProcessingQueue.class));
        verify(auditRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                "RECEIVE".equals(log.getStage()) && log.getStatus() == EdiAuditStatus.SUCCESS
        ));

        listener.onEdiMessageProcessed(new EdiMessageProcessedEvent(10L, EdiMessageType.ORDERS, EdiMessageStatus.FAILED, null, "bad payload", LocalDateTime.now()));

        verify(auditRepository).save(org.mockito.ArgumentMatchers.argThat((EdiAuditLog log) ->
                "PROCESS".equals(log.getStage()) && log.getStatus() == EdiAuditStatus.FAILED && "bad payload".equals(log.getDetails())
        ));

        when(messageRepository.findById(99L)).thenReturn(Optional.empty());
        listener.onEdiMessageReceived(new EdiMessageReceivedEvent(99L, EdiMessageType.ORDERS, 1L, "missing", LocalDateTime.now()));
        verify(queueRepository, never()).existsByEdiMessage_Id(99L);
    }

    @Test
    void reportCacheEvictListenerClearsOnlyReportCaches() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache reportCache = mock(Cache.class);
        Cache dashboardCache = mock(Cache.class);
        ReportCacheEvictListener listener = new ReportCacheEvictListener(cacheManager);

        when(cacheManager.getCacheNames()).thenReturn(Set.of("report-turnover", "dashboard", "users"));
        when(cacheManager.getCache("report-turnover")).thenReturn(reportCache);
        when(cacheManager.getCache("dashboard")).thenReturn(dashboardCache);

        listener.onOperationCompleted(new OperationCompletedEvent(1L, "OP-1", OperationType.INCOME, 1L, "manager", LocalDateTime.now()));

        verify(reportCache).clear();
        verify(dashboardCache).clear();
        verify(cacheManager, never()).getCache("users");
    }

    private User user(String username, boolean active) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(UserRole.MANAGER);
        user.setIsActive(active);
        return user;
    }
}
