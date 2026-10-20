package br.com.nandesk.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI 3.1 (8.1); versionado em {@code docs/api/openapi.json} pelo {@code OpenApiSnapshotIT}. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI nandeskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NANDESK API")
                        .version("v1")
                        .description("Plataforma colaborativa das feiras de Belém (Ver-o-Peso e Feira da 25).")
                        .contact(new Contact().name("Equipe NANDESK").email("contato@nandesk.com.br"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
