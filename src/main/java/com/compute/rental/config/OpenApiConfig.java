package com.compute.rental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI computeRentalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("算力租赁平台 API")
                        .version("v1")
                        .description("GPU 算力租赁、API 激活、钱包、收益和佣金后台接口"))
                .schemaRequirement(JWT_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME));
    }
}
