package com.example.servicehub_api.controller;

import com.example.servicehub_api.dto.LoginRequestDTO;
import com.example.servicehub_api.dto.RegisterRequestDTO;
import com.example.servicehub_api.dto.ClienteResponseDTO;
import com.example.servicehub_api.model.UserRole;
import com.example.servicehub_api.service.AccountService;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.example.servicehub_api.dto.LoginResponseDTO;
import com.example.servicehub_api.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accounts;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, AccountService accounts) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cadastro público não permite ADMIN");
        }
        return ClienteResponseDTO.from(accounts.criar(request, request.getRole()));
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            String token = jwtService.generateToken((org.springframework.security.core.userdetails.UserDetails)
                    authentication.getPrincipal());
            return new LoginResponseDTO(token, "Bearer", jwtService.getExpirationSeconds(), roles);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }
    }
}
