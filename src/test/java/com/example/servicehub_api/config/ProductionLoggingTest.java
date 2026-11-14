package com.example.servicehub_api.config;

import com.example.servicehub_api.exception.GlobalExceptionHandler;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class ProductionLoggingTest {
    @Test
    void productionSuppressesRawJdbcDetailsAndKeepsCorrelatedDiagnostics(CapturedOutput output) {
        var application = new SpringApplication(LoggingOnlyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        try (var context = application.run("--spring.profiles.active=prod", "--spring.config.import=", "--spring.main.banner-mode=off")) {
            String sensitive = "private-value-never-log@example.invalid";
            MDC.put(RequestIdFilter.MDC_KEY, "prod-jdbc-privacy");
            new SqlExceptionHelper(false).logExceptions(new SQLException("Key (email)=(" + sensitive + ") already exists", "23505"), "SQL failure");
            var handler = new GlobalExceptionHandler();
            assertEquals(409, handler.integrityViolation().getStatusCode().value());
            assertEquals(500, handler.generic(new DataAccessResourceFailureException(sensitive)).getStatusCode().value());
            assertTrue(context.isActive());
            assertEquals("ecs", context.getEnvironment().getProperty("logging.structured.format.console"));
            String logs = output.getAll();
            assertFalse(logs.contains(sensitive));
            assertTrue(logs.contains("DataAccessResourceFailureException"));
            // Outros contextos Spring da suíte podem já ter inicializado o console.
            // O formato ECS em execução é verificado também na bancada do JAR prod.
            assertTrue(logs.contains("prod-jdbc-privacy"));
        } finally {
            MDC.remove(RequestIdFilter.MDC_KEY);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LoggingOnlyConfiguration {}
}
