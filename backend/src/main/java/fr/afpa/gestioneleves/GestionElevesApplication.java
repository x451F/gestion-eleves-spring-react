package fr.afpa.gestioneleves;

import fr.afpa.gestioneleves.config.StorageProperties;
import fr.afpa.gestioneleves.config.CorsProperties;
import fr.afpa.gestioneleves.config.MailProperties;
import fr.afpa.gestioneleves.security.JwtProperties;
import fr.afpa.gestioneleves.security.RefreshCookieProperties;
import fr.afpa.gestioneleves.service.BootstrapCommandMode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, CorsProperties.class, JwtProperties.class, RefreshCookieProperties.class, MailProperties.class})
public class GestionElevesApplication {

    public static void main(String[] args) {
        if (args.length > 0 && "bootstrap-admin".equals(args[0])) {
            SpringApplication application = bootstrapCommandApplication();
            ConfigurableApplicationContext context = application.run(java.util.Arrays.copyOfRange(args, 1, args.length));
            System.exit(SpringApplication.exit(context));
        }
        SpringApplication.run(GestionElevesApplication.class, args);
    }

    /** Creates the application used only by the explicit bootstrap-admin command. */
    public static SpringApplication bootstrapCommandApplication() {
        SpringApplication application = new SpringApplication(GestionElevesApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.addInitializers(context -> context.getBeanFactory()
                .registerSingleton(BootstrapCommandMode.BEAN_NAME, new BootstrapCommandMode()));
        return application;
    }
}
