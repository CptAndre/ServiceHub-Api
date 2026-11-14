package com.example.servicehub_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.json.JsonCompareMode;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VersionedApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Value("${app.version}") String version;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configure(properties);
    }

    @Test
    void healthIsPublicWithoutExposingDatabaseDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status":"UP","groups":["liveness","readiness"]}
                        """, JsonCompareMode.STRICT))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void infoUsesTheArtifactVersionAndPreservesTheLegacyRoute() throws Exception {
        mvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(version))
                .andExpect(header().doesNotExist("Deprecation"));
        mvc.perform(get("/api/hello"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(version))
                .andExpect(header().string("Deprecation", startsWith("@")))
                .andExpect(header().doesNotExist("Sunset"));
    }

    @Test
    void versionedBusinessRoutesStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiSeparatesV1AndDocumentsBearerAuthentication() throws Exception {
        mvc.perform(get("/api-docs/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value(version))
                .andExpect(jsonPath("$.paths['/api/v1/clientes']").exists())
                .andExpect(jsonPath("$.paths['/api/clientes']").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/clientes'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist());
    }
}
