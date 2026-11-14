package com.example.servicehub_api.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Component
@Profile("prod")
public class ProductionConfiguration {
    public ProductionConfiguration(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.url}") String databaseUrl,
            @Value("${spring.datasource.username}") String databaseUser,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${app.cors.allowed-origins}") String origins) {
        if (!databaseUrl.startsWith("jdbc:postgresql://") || databaseUser.isBlank() || databasePassword.isBlank()) {
            throw new IllegalArgumentException("Produção requer DB_URL JDBC PostgreSQL, DB_USER e DB_PASS");
        }
        byte[] key;
        try {
            key = Decoders.BASE64.decode(jwtSecret);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("JWT_SECRET deve ser Base64 válido");
        }
        byte[] demo = Decoders.BASE64.decode("U2VydmljZUh1Yl9kZXZfb25seV9rZXlfMzJfYnl0ZXNfbWluaW11bQ==");
        if (key.length < 32 || Arrays.equals(key, demo)) {
            throw new IllegalArgumentException("Produção requer JWT_SECRET próprio, com pelo menos 32 bytes");
        }
        if (origins.isBlank()) throw new IllegalArgumentException("Defina CORS_ALLOWED_ORIGINS em produção");
        for (String origin : origins.split(",", -1)) {
            URI uri = URI.create(origin.trim());
            if (uri.getHost() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))) {
                throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS deve conter somente origens HTTP(S) explícitas");
            }
        }
    }
}
