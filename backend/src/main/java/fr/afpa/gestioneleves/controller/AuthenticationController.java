package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.LoginRequest;
import fr.afpa.gestioneleves.dto.response.CurrentUserResponse;
import fr.afpa.gestioneleves.dto.response.LoginResponse;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import fr.afpa.gestioneleves.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody(required = false) LoginRequest request) {
        return authenticationService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return new CurrentUserResponse(user.id(), user.email(), user.role(), user.status());
    }
}
