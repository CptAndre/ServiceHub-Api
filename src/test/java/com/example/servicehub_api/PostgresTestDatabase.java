package com.example.servicehub_api;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

/** CI requires a real PostgreSQL; local runs may use a dedicated existing database. */
public final class PostgresTestDatabase {
    private PostgresTestDatabase() {}

    public static boolean isEnabled() {
        return hasExternalDatabase() || Boolean.parseBoolean(System.getenv("SERVICEHUB_TESTCONTAINERS"));
    }

    private static boolean hasExternalDatabase() {
        String url = System.getenv("SERVICEHUB_TEST_DB_URL");
        return url != null && !url.isBlank();
    }

    /** Lazy singleton: Testcontainers/Ryuk owns cleanup at JVM shutdown. */
    private static final class ContainerHolder {
        private static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("servicehub_test")
                    .withUsername("servicehub_test")
                    .withPassword(UUID.randomUUID().toString());

        static {
            INSTANCE.start();
        }
    }

    public static String url() {
        return hasExternalDatabase() ? System.getenv("SERVICEHUB_TEST_DB_URL") : ContainerHolder.INSTANCE.getJdbcUrl();
    }

    public static String username() {
        return hasExternalDatabase() ? System.getenv().getOrDefault("SERVICEHUB_TEST_DB_USER", "servicehub_test") : ContainerHolder.INSTANCE.getUsername();
    }

    public static String password() {
        return hasExternalDatabase() ? System.getenv().getOrDefault("SERVICEHUB_TEST_DB_PASSWORD", "") : ContainerHolder.INSTANCE.getPassword();
    }

    public static void configure(DynamicPropertyRegistry properties) {
        if (!isEnabled()) {
            if (Boolean.parseBoolean(System.getenv("SERVICEHUB_REQUIRE_POSTGRES"))) {
                throw new IllegalStateException("CI requer PostgreSQL real; configure Testcontainers ou SERVICEHUB_TEST_DB_URL");
            }
            return;
        }
        properties.add("spring.datasource.url", PostgresTestDatabase::url);
        properties.add("spring.datasource.username", PostgresTestDatabase::username);
        properties.add("spring.datasource.password", PostgresTestDatabase::password);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
    }
}
