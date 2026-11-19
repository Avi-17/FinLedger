package com.finledger.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI document behind Swagger UI (/swagger-ui.html).
 * Declares JWT bearer auth once, so Swagger shows an "Authorize" button and sends the token on every call.
 * Endpoints that don't need a token opt out with @SecurityRequirements (empty).
 */
@Configuration
public class OpenApiConfig {
    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI finLedgerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinLedger API")
                        .version("1.0")
                        .description("""
                                Google Pay–style bill splitting with Razorpay (test mode, no real money).

                                **Try it:** `POST /auth/login` → copy `token` → **Authorize** (top right) → call any endpoint.
                                Test users: rahul / priya / aman `@test.com`, password `password123`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste the token from /auth/login (without the word Bearer)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                // Tag order = section order in Swagger UI, following the demo flow
                .tags(List.of(
                        new Tag().name("Auth").description("Register, log in, current user"),
                        new Tag().name("Groups").description("Groups and their members"),
                        new Tag().name("Expenses").description("Split requests (Google Pay style): create, feed, mark as paid, close"),
                        new Tag().name("Payments").description("Pay your share through Razorpay (test mode)"),
                        new Tag().name("Webhooks").description("Called by Razorpay, not by users")));
    }
}
