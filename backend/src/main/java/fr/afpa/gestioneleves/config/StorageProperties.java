package fr.afpa.gestioneleves.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(Path photoRoot, long maxPhotoSize) {
}
