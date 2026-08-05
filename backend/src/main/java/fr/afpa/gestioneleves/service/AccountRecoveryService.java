package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.entity.ActivationToken;
import fr.afpa.gestioneleves.entity.PasswordResetToken;
import fr.afpa.gestioneleves.entity.SecurityEvent;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.InvalidAccountTokenException;
import fr.afpa.gestioneleves.repository.ActivationTokenRepository;
import fr.afpa.gestioneleves.repository.PasswordResetTokenRepository;
import fr.afpa.gestioneleves.repository.SecurityEventRepository;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.OpaqueTokenService;
import fr.afpa.gestioneleves.security.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AccountRecoveryService {
    private static final String ACTIVATED_REASON = "ACTIVATED";
    private static final String REPLACED_REASON = "REPLACED";
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UtilisateurRepository userRepository;
    private final SecurityEventRepository eventRepository;
    private final OpaqueTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final RefreshSessionService refreshSessionService;
    private final Clock clock;

    public AccountRecoveryService(ActivationTokenRepository activationTokenRepository,
                                  PasswordResetTokenRepository resetTokenRepository, UtilisateurRepository userRepository,
                                  SecurityEventRepository eventRepository, OpaqueTokenService tokenService,
                                  PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                                  RefreshSessionService refreshSessionService, Clock clock) {
        this.activationTokenRepository = activationTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.refreshSessionService = refreshSessionService;
        this.clock = clock;
    }

    @Transactional
    public void activate(String rawToken, String password) {
        requireValidPassword(password);
        ActivationToken token = activationTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidAccountTokenException::new);
        Utilisateur user = userRepository.findByIdForUpdate(token.getUtilisateur().getId())
                .orElseThrow(InvalidAccountTokenException::new);
        LocalDateTime now = now();
        if (!usable(token.getUsedAt(), token.getRevokedAt(), token.getExpiresAt(), now)
                || user.getStatut() != StatutUtilisateur.EN_ATTENTE_ACTIVATION) {
            throw new InvalidAccountTokenException();
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatut(StatutUtilisateur.ACTIF);
        user.setEmailVerifieAt(now);
        token.setUsedAt(now);
        activationTokenRepository.revokeUnusedForUser(user.getId(), now, ACTIVATED_REASON);
        record(user, SecurityEventType.ACTIVATION_USED);
    }

    @Transactional
    public Optional<IssuedReset> requestPasswordReset(String submittedEmail) {
        String email = canonicalEmail(submittedEmail);
        if (email == null) return Optional.empty();
        Optional<Utilisateur> found = userRepository.findByEmailNormalise(email);
        if (found.isEmpty()) return Optional.empty();
        Utilisateur user = userRepository.findByIdForUpdate(found.get().getId()).orElse(null);
        if (user == null || user.getStatut() != StatutUtilisateur.ACTIF) return Optional.empty();
        LocalDateTime now = now();
        resetTokenRepository.revokeUnusedForUser(user.getId(), now, REPLACED_REASON);
        String rawToken = tokenService.generate();
        PasswordResetToken token = new PasswordResetToken();
        token.setUtilisateur(user);
        token.setTokenHash(tokenService.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(30));
        resetTokenRepository.save(token);
        record(user, SecurityEventType.PASSWORD_RESET_REQUESTED);
        return Optional.of(new IssuedReset(user, rawToken));
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        requireValidPassword(password);
        PasswordResetToken token = resetTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidAccountTokenException::new);
        Utilisateur user = userRepository.findByIdForUpdate(token.getUtilisateur().getId())
                .orElseThrow(InvalidAccountTokenException::new);
        LocalDateTime now = now();
        if (!usable(token.getUsedAt(), token.getRevokedAt(), token.getExpiresAt(), now)
                || user.getStatut() != StatutUtilisateur.ACTIF) {
            throw new InvalidAccountTokenException();
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        token.setUsedAt(now);
        resetTokenRepository.revokeUnusedForUser(user.getId(), now, "RESET_USED");
        refreshSessionService.revokeAllForUser(user.getId());
        record(user, SecurityEventType.PASSWORD_RESET_USED);
    }

    private String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidAccountTokenException();
        return tokenService.hash(rawToken);
    }

    private boolean usable(LocalDateTime usedAt, LocalDateTime revokedAt, LocalDateTime expiresAt, LocalDateTime now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    private void requireValidPassword(String password) {
        if (!passwordPolicy.isValid(password)) {
            throw new BusinessRuleException("Le mot de passe doit comporter entre 12 et 64 caractères.");
        }
    }

    private String canonicalEmail(String email) {
        if (email == null) return null;
        String canonical = email.trim().toLowerCase(Locale.ROOT);
        return canonical.isBlank() ? null : canonical;
    }

    private void record(Utilisateur user, SecurityEventType type) {
        SecurityEvent event = new SecurityEvent();
        event.setUtilisateur(user);
        event.setEventType(type);
        event.setOccurredAt(now());
        eventRepository.save(event);
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), clock.getZone()); }

    public record IssuedReset(Utilisateur user, String rawToken) {
    }
}
