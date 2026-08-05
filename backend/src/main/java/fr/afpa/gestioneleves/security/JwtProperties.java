package fr.afpa.gestioneleves.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String secretBase64,
        @NotNull Duration accessTokenLifetime
) {
    public JwtProperties {
        if (!Duration.ofMinutes(15).equals(accessTokenLifetime)) {
            throw new IllegalArgumentException("Access token lifetime must be exactly 15 minutes");
        }
    }
}
