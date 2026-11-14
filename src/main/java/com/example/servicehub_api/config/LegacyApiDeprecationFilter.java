package com.example.servicehub_api.config;

import jakarta.servlet.FilterChain;
import org.jspecify.annotations.NonNull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LegacyApiDeprecationFilter extends OncePerRequestFilter {
    private final Instant deprecatedAt;
    private final Instant sunsetAt;

    public LegacyApiDeprecationFilter(
            @Value("${app.api.legacy.deprecation-date}") String deprecatedAt,
            @Value("${app.api.legacy.sunset-date:}") String sunsetAt) {
        this.deprecatedAt = Instant.parse(deprecatedAt);
        this.sunsetAt = sunsetAt.isBlank() ? null : Instant.parse(sunsetAt);
        if (this.sunsetAt != null && this.sunsetAt.isBefore(this.deprecatedAt.plus(Duration.ofDays(90)))) {
            throw new IllegalArgumentException("Sunset deve respeitar ao menos 90 dias após a depreciação");
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/") && !path.startsWith("/api/v1/")) {
            response.setHeader("Deprecation", "@" + deprecatedAt.getEpochSecond());
            response.addHeader("Link", "</api-docs/v1>; rel=\"describedby\"; type=\"application/json\"");
            if (sunsetAt != null) {
                response.setHeader("Sunset", DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                        .withZone(ZoneOffset.UTC).format(sunsetAt));
            }
        }
        filterChain.doFilter(request, response);
    }
}
