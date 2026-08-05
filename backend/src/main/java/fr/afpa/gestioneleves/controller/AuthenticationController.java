package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.LoginRequest;
import fr.afpa.gestioneleves.dto.response.CurrentUserResponse;
import fr.afpa.gestioneleves.dto.response.LoginResponse;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import fr.afpa.gestioneleves.security.AccessTokenService;
import fr.afpa.gestioneleves.security.RefreshCookieService;
import fr.afpa.gestioneleves.security.AuthenticationFailedException;
import fr.afpa.gestioneleves.security.RefreshAuthenticationFailedException;
import fr.afpa.gestioneleves.service.AuthenticationService;
import fr.afpa.gestioneleves.service.RefreshSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final RefreshSessionService refreshSessionService;
    private final AccessTokenService accessTokenService;
    private final RefreshCookieService refreshCookieService;

    public AuthenticationController(AuthenticationService authenticationService,
                                    RefreshSessionService refreshSessionService,
                                    AccessTokenService accessTokenService,
                                    RefreshCookieService refreshCookieService) {
        this.authenticationService = authenticationService;
        this.refreshSessionService = refreshSessionService;
        this.accessTokenService = accessTokenService;
        this.refreshCookieService = refreshCookieService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponse> login(@RequestBody(required = false) LoginRequest request) {
        AuthenticationService.LoginResult login = authenticationService.login(request);
        HttpHeaders headers = new HttpHeaders();
        refreshCookieService.addIssuedCookie(headers, login.refreshToken());
        return ResponseEntity.ok().headers(headers).body(login.response());
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        RefreshSessionService.IssuedRefreshToken refresh;
        try {
            refresh = refreshSessionService.rotate(refreshToken(request));
        } catch (AuthenticationFailedException ex) {
            throw new RefreshAuthenticationFailedException();
        }
        var token = accessTokenService.issue(refresh.utilisateur());
        HttpHeaders headers = new HttpHeaders();
        refreshCookieService.addIssuedCookie(headers, refresh.rawToken());
        return ResponseEntity.ok().headers(headers)
                .body(new LoginResponse(token.value(), "Bearer", token.expiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        refreshSessionService.logout(refreshToken(request));
        HttpHeaders headers = new HttpHeaders();
        refreshCookieService.addClearedCookie(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser user) {
        refreshSessionService.logoutAll(user.id());
        HttpHeaders headers = new HttpHeaders();
        refreshCookieService.addClearedCookie(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return new CurrentUserResponse(user.id(), user.email(), user.role(), user.status());
    }

    private String refreshToken(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, refreshCookieService.name());
        return cookie == null ? null : cookie.getValue();
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
