package fr.afpa.gestioneleves;

import fr.afpa.gestioneleves.config.StorageProperties;
import fr.afpa.gestioneleves.config.CorsProperties;
import fr.afpa.gestioneleves.security.JwtProperties;
import fr.afpa.gestioneleves.security.RefreshCookieProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, CorsProperties.class, JwtProperties.class, RefreshCookieProperties.class})
public class GestionElevesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionElevesApplication.class, args);
    }
}
