package fr.afpa.gestioneleves.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.refresh-cookie")
public record RefreshCookieProperties(
        @NotBlank String name,
        @NotBlank String path,
        @NotNull Duration lifetime,
        boolean secure,
        @NotBlank String sameSite
) {
    public RefreshCookieProperties {
        if (!"/api/auth".equals(path)) {
            throw new IllegalArgumentException("Refresh cookie path must be /api/auth");
        }
        if (!Duration.ofDays(7).equals(lifetime)) {
            throw new IllegalArgumentException("Refresh cookie lifetime must be exactly 7 days");
        }
        if (!"Strict".equals(sameSite)) {
            throw new IllegalArgumentException("Refresh cookie SameSite must be Strict");
        }
    }
}
