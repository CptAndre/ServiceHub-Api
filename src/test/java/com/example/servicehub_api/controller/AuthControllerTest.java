package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.LoginRequestDTO;
import com.example.servicehub_api.service.AccountService;
import com.example.servicehub_api.service.JwtService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "unexpected-principal")
    void rejectsNullOrUnsupportedPrincipalWithoutIssuingToken(@Nullable String principal) {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        Authentication authentication = mock(Authentication.class);
        JwtService jwt = mock(JwtService.class);
        when(manager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        var controller = new AuthController(manager, jwt, mock(AccountService.class));
        var error = assertThrows(ResponseStatusException.class,
                () -> controller.login(new LoginRequestDTO("client@example.com", "password")));
        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(jwt);
    }
}
