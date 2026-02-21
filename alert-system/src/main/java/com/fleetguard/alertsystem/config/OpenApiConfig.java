package com.fleetguard.alertsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fleetGuardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FleetGuard — Intelligent Alert Escalation & Resolution System")
                        .description("""
                                REST API for fleet-monitoring alert ingestion, rule-based escalation,
                                automated lifecycle management, and analytics dashboards.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FleetGuard Team")
                                .email("support@fleetguard.io")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
