package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.LoginRequest;
import fr.afpa.gestioneleves.dto.response.LoginResponse;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.AccessTokenService;
import fr.afpa.gestioneleves.security.AuthenticationFailedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {
    private static final String DUMMY_PASSWORD = "invalid-password-for-timing-only";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthenticationService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
                                 AccessTokenService accessTokenService, Clock clock) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    public LoginResponse login(LoginRequest request) {
        String email = canonicalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();
        Optional<Utilisateur> account = email == null ? Optional.empty() : utilisateurRepository.findByEmailNormalise(email);
        String hash = account.map(Utilisateur::getPasswordHash).orElse(dummyPasswordHash);
        boolean matches = password != null && safelyMatches(password, hash);

        Utilisateur utilisateur = account.orElse(null);
        if (!matches || utilisateur == null || utilisateur.getStatut() != StatutUtilisateur.ACTIF) {
            throw new AuthenticationFailedException();
        }

        utilisateur.setLastLoginAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        AccessTokenService.IssuedAccessToken token = accessTokenService.issue(utilisateur);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    private boolean safelyMatches(String password, String hash) {
        try {
            return passwordEncoder.matches(password, hash);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String canonicalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
