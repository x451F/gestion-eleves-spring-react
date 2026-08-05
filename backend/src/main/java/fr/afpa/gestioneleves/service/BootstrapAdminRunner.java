package fr.afpa.gestioneleves.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnNotWebApplication
public class BootstrapAdminRunner implements ApplicationRunner {
    private final AccountProvisioningService provisioningService;
    private final AccountMailService mailService;
    private final Environment environment;
    private final ObjectProvider<BootstrapCommandMode> commandMode;

    public BootstrapAdminRunner(AccountProvisioningService provisioningService, AccountMailService mailService,
                                Environment environment, ObjectProvider<BootstrapCommandMode> commandMode) {
        this.provisioningService = provisioningService;
        this.mailService = mailService;
        this.environment = environment;
        this.commandMode = commandMode;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (commandMode.getIfAvailable() == null) {
            return;
        }
        String email = environment.getProperty("app.bootstrap-admin.email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L’option --app.bootstrap-admin.email est requise.");
        }
        AccountProvisioningService.ProvisionedAccount account = provisioningService.bootstrapAdmin(email);
        boolean delivered = mailService.sendActivation(account.user(), account.rawActivationToken());
        System.out.printf("Compte ADMIN en attente créé pour %s. Livraison de l’e-mail : %s.%n",
                account.user().getEmailNormalise(), delivered ? "réussie" : "échouée");
    }
}
