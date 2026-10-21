package ma.atlasrh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la documentation OpenAPI (Swagger UI).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI atlasrhOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AtlasRH API")
                        .description("API REST de gestion des ressources humaines : départements, employés et contrats.")
                        .version("1.0"));
    }
}
