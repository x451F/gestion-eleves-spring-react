package fr.afpa.gestioneleves;

import fr.afpa.gestioneleves.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class GestionElevesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionElevesApplication.class, args);
    }
}
