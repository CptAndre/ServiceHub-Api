package com.example.servicehub_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI serviceHubOpenAPI(@Value("${app.version}") String version) {
        return new OpenAPI()
                .info(new Info().title("ServiceHub API").version(version)
                        .description("Clientes, prestadores, serviços, ordens, avaliações e pagamentos. Contrato REST v1."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1/**").build();
    }

    @Bean
    public GlobalOpenApiCustomizer legacyOperations() {
        return api -> {
            if (api.getPaths() == null) return;
            api.getPaths().forEach((path, item) -> {
                if (path.startsWith("/api/") && !path.startsWith("/api/v1/")) {
                    item.readOperations().forEach(operation -> operation.setDeprecated(true));
                }
            });
        };
    }
}
