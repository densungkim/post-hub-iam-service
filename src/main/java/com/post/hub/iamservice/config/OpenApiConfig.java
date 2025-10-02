package com.post.hub.iamservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "POST_HUB REST API",
                version = "1.0",
                description = """
                        IAM-service REST API

                        You can test this service using the following demo accounts:
                              \n- super_admin@gmail.com | password1
                              \n- admin@gmail.com       | password2
                              \n- user@gmail.com        | password3
                        
                        Use these credentials to obtain a JWT token.
                        This token is required for secured endpoints in IAM Service itself\s
                        and is also mandatory when accessing the Utils Service.
                        """
        ),
        security = {@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)}
)
@SecurityScheme(
        name = HttpHeaders.AUTHORIZATION,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        SpringDocUtils.getConfig().replaceWithClass(LocalDateTime.class, Long.class);
        SpringDocUtils.getConfig().replaceWithClass(LocalDate.class, Long.class);
        SpringDocUtils.getConfig().replaceWithClass(Date.class, Long.class);

        SpringDocUtils.getConfig().addResponseTypeToIgnore(GrantedAuthority.class);

        return GroupedOpenApi.builder()
                .group("iam-service")
                .packagesToScan("com.post.hub.iamservice")
                .build();
    }

}
