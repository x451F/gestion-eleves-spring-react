package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.ProvisionGuardianAccountRequest;
import fr.afpa.gestioneleves.dto.request.ProvisionTeacherAccountRequest;
import fr.afpa.gestioneleves.dto.response.ProvisionedAccountResponse;
import fr.afpa.gestioneleves.service.AccountMailService;
import fr.afpa.gestioneleves.service.AccountProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {
    private final AccountProvisioningService provisioningService;
    private final AccountMailService mailService;

    public AdminAccountController(AccountProvisioningService provisioningService, AccountMailService mailService) {
        this.provisioningService = provisioningService;
        this.mailService = mailService;
    }

    @PostMapping("/teachers")
    public ResponseEntity<ProvisionedAccountResponse> provisionTeacher(@Valid @RequestBody ProvisionTeacherAccountRequest request) {
        AccountProvisioningService.ProvisionedAccount account = provisioningService.provisionTeacher(request);
        boolean delivered = mailService.sendActivation(account.user(), account.rawActivationToken());
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + account.user().getId()))
                .body(response(account, delivered));
    }

    @PostMapping("/guardians")
    public ResponseEntity<ProvisionedAccountResponse> provisionGuardian(@Valid @RequestBody ProvisionGuardianAccountRequest request) {
        AccountProvisioningService.ProvisionedAccount account = provisioningService.provisionGuardian(request);
        boolean delivered = mailService.sendActivation(account.user(), account.rawActivationToken());
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + account.user().getId()))
                .body(response(account, delivered));
    }

    @PostMapping("/{id}/resend-activation")
    public ResponseEntity<ProvisionedAccountResponse> resendActivation(@PathVariable Long id) {
        AccountProvisioningService.ProvisionedAccount account = provisioningService.resendActivation(id);
        boolean delivered = mailService.sendActivation(account.user(), account.rawActivationToken());
        return ResponseEntity.status(HttpStatus.OK).body(response(account, delivered));
    }

    private ProvisionedAccountResponse response(AccountProvisioningService.ProvisionedAccount account, boolean delivered) {
        return new ProvisionedAccountResponse(account.user().getId(), account.user().getEmailNormalise(), account.user().getRole(),
                account.user().getStatut(), account.profileId(), delivered);
    }
}
