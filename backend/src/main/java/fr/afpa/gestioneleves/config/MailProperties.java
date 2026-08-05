package fr.afpa.gestioneleves.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(@NotBlank @Email String sender, @NotBlank String activationUrl,
                             @NotBlank String resetUrl) {
}
