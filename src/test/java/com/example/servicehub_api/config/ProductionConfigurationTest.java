package com.example.servicehub_api.config;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTest {
    private final String validKey = Base64.getEncoder().encodeToString(new byte[32]);

    @Test void acceptsExplicitProductionConfiguration() {
        assertDoesNotThrow(() -> new ProductionConfiguration(validKey,
                "jdbc:postgresql://db:5432/servicehub", "app", "test-password", "https://app.example.com"));
    }

    @Test void rejectsTheDevelopmentSigningKey() {
        assertThrows(IllegalArgumentException.class, () -> new ProductionConfiguration(
                "U2VydmljZUh1Yl9kZXZfb25seV9rZXlfMzJfYnl0ZXNfbWluaW11bQ==",
                "jdbc:postgresql://db:5432/servicehub", "app", "test-password", "https://app.example.com"));
    }

    @Test void rejectsMissingCredentialsAndNonJdbcUrls() {
        assertThrows(IllegalArgumentException.class, () -> new ProductionConfiguration(validKey,
                "postgres://db/servicehub", "app", "test-password", "https://app.example.com"));
        assertThrows(IllegalArgumentException.class, () -> new ProductionConfiguration(validKey,
                "jdbc:postgresql://db/servicehub", "app", "", "https://app.example.com"));
    }

    @Test void rejectsWildcardAndUnconfiguredCorsOrigins() {
        for (String origins : new String[]{"", "*", "https://app.example.com,"}) {
            assertThrows(IllegalArgumentException.class, () -> new ProductionConfiguration(validKey,
                    "jdbc:postgresql://db/servicehub", "app", "test-password", origins));
        }
    }
}
