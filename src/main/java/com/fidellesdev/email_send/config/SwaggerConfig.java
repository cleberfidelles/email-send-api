package com.fidellesdev.email_send.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String ESQUEMA_API_KEY = "apiKey";

    @Bean
    public OpenAPI customSwagger() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aplicativo Email Send")
                        .version("1.0.0")
                        .description("API REST para envio assíncrono de e-mails (Kafka)"))
                .components(new Components().addSecuritySchemes(ESQUEMA_API_KEY,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ApiKeyFilter.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_API_KEY));
    }
}
