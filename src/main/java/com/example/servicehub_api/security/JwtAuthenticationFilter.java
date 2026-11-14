package com.example.servicehub_api.security;

import com.example.servicehub_api.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            try {
                Claims claims = jwtService.parseClaims(token);
                String username = claims.getSubject();
                if (username != null && !username.isBlank()) {
                    UserDetails user = userDetailsService.loadUserByUsername(username);
                    Set<String> tokenRoles = readRoles(claims);
                    Set<String> currentRoles = user.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    if (user.isEnabled() && user.isAccountNonLocked() && user.isAccountNonExpired()
                            && user.isCredentialsNonExpired() && currentRoles.size() == user.getAuthorities().size()
                            && tokenRoles.equals(currentRoles)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException | org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private Set<String> readRoles(Claims claims) {
        Object value = claims.get("roles");
        if (!(value instanceof List<?> roles) || roles.isEmpty()) {
            throw new IllegalArgumentException("Perfis ausentes ou inválidos no token");
        }
        Set<String> validated = new HashSet<>();
        for (Object role : roles) {
            if (!(role instanceof String name) || name.isBlank() || !validated.add(name)) {
                throw new IllegalArgumentException("Perfis inválidos no token");
            }
        }
        return validated;
    }
}
