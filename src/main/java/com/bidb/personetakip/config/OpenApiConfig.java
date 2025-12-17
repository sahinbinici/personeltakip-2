package com.bidb.personetakip.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Personnel Tracking System.
 * Configures Swagger UI with API metadata, security schemes, and server information.
 * 
 * Requirements: 1.1, 4.5 - API documentation interface and metadata
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Configures the OpenAPI specification with comprehensive API metadata.
     * 
     * @return OpenAPI configuration with info, security, and servers
     * Requirements: 1.1 - Comprehensive API documentation interface
     *               4.5 - API metadata and contact information
     */
    @Bean
    public OpenAPI personnelTrackingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personnel Tracking System API")
                        .description("REST API for personnel entry/exit tracking system with QR code authentication, " +
                                   "admin management, and comprehensive reporting capabilities.")
                        .version(appVersion)
                        .contact(new Contact()
                                .name("BIDB Development Team")
                                .email("dev@bidb.com")
                                .url("https://bidb.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://bidb.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("https://api.personeltakip.com")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authentication - Enter your JWT token")));
    }
}