package com.optimaxx.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OptiMaxx Management API")
                        .version("v1")
                        .description("Foundation API for admin and sales portals")
                        .license(new License().name("Proprietary")));
    }
}
