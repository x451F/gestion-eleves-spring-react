package fr.afpa.gestioneleves.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotNull @NotEmpty List<String> allowedOrigins) {
    public CorsProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
        if (allowedOrigins.stream().anyMatch(origin -> origin == null || origin.isBlank() || origin.contains("*"))) {
            throw new IllegalArgumentException("CORS origins must be explicit, non-wildcard origins");
        }
    }
}
