package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.entity.RefreshSession;
import fr.afpa.gestioneleves.entity.RefreshSessionFamily;
import fr.afpa.gestioneleves.entity.SecurityEvent;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.repository.RefreshSessionFamilyRepository;
import fr.afpa.gestioneleves.repository.RefreshSessionRepository;
import fr.afpa.gestioneleves.repository.SecurityEventRepository;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.AuthenticationFailedException;
import fr.afpa.gestioneleves.security.RefreshCookieProperties;
import fr.afpa.gestioneleves.security.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefreshSessionService {
    private static final String LOGOUT_REASON = "LOGOUT";
    private static final String LOGOUT_ALL_REASON = "LOGOUT_ALL";
    private static final String REUSE_REASON = "TOKEN_REUSE";
    private static final String GLOBAL_REVOCATION_REASON = "GLOBAL_REVOCATION";

    private final RefreshSessionRepository sessionRepository;
    private final RefreshSessionFamilyRepository familyRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SecurityEventRepository securityEventRepository;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieProperties cookieProperties;
    private final Clock clock;

    public RefreshSessionService(RefreshSessionRepository sessionRepository,
                                 RefreshSessionFamilyRepository familyRepository,
                                 UtilisateurRepository utilisateurRepository,
                                 SecurityEventRepository securityEventRepository,
                                 RefreshTokenService refreshTokenService,
                                 RefreshCookieProperties cookieProperties,
                                 Clock clock) {
        this.sessionRepository = sessionRepository;
        this.familyRepository = familyRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.securityEventRepository = securityEventRepository;
        this.refreshTokenService = refreshTokenService;
        this.cookieProperties = cookieProperties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken createFamily(Utilisateur utilisateur) {
        LocalDateTime now = now();
        LocalDateTime expiresAt = now.plus(cookieProperties.lifetime());
        RefreshSessionFamily family = new RefreshSessionFamily();
        family.setUtilisateur(utilisateur);
        family.setCreatedAt(now);
        family.setExpiresAt(expiresAt);
        familyRepository.save(family);
        return persistSession(family, 0, now, expiresAt);
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public IssuedRefreshToken rotate(String rawToken) {
        RefreshSession session = findLockedSession(rawToken);
        RefreshSessionFamily family = findLockedFamily(session);
        LocalDateTime now = now();

        if (isReused(session)) {
            revokeFamily(family, REUSE_REASON, true, now);
            throw new AuthenticationFailedException();
        }
        if (!isUsable(session, family, now)
                || sessionRepository.existsByFamilyIdAndGenerationGreaterThan(family.getId(), session.getGeneration())) {
            throw new AuthenticationFailedException();
        }

        IssuedRefreshToken replacement = persistSession(family, session.getGeneration() + 1, now, family.getExpiresAt());
        session.setUsedAt(now);
        session.assignReplacement(replacement.sessionId(), replacement.generation());
        family.setLastUsedAt(now);
        return replacement;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessionRepository.findByTokenHashForUpdate(refreshTokenService.hash(rawToken)).ifPresent(session -> {
            RefreshSessionFamily family = findLockedFamily(session);
            revokeFamily(family, LOGOUT_REASON, false, now());
        });
    }

    @Transactional
    public void logoutAll(Long userId) {
        revokeAllForUser(userId, LOGOUT_ALL_REASON);
    }

    /**
     * Revokes all session families and invalidates all access tokens for use by future
     * password-reset, account-deactivation, and security-response workflows.
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        revokeAllForUser(userId, GLOBAL_REVOCATION_REASON);
    }

    private void revokeAllForUser(Long userId, String reason) {
        Utilisateur utilisateur = utilisateurRepository.findByIdForUpdate(userId)
                .orElseThrow(AuthenticationFailedException::new);
        LocalDateTime now = now();
        List<RefreshSessionFamily> families = familyRepository.findByUtilisateurIdForUpdate(utilisateur.getId());
        for (RefreshSessionFamily family : families) {
            revokeFamily(family, reason, false, now);
        }
        utilisateur.setTokenVersion(Math.addExact(utilisateur.getTokenVersion(), 1));
    }

    private RefreshSession findLockedSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        return sessionRepository.findByTokenHashForUpdate(refreshTokenService.hash(rawToken))
                .orElseThrow(AuthenticationFailedException::new);
    }

    private RefreshSessionFamily findLockedFamily(RefreshSession session) {
        return familyRepository.findByIdForUpdate(session.getFamily().getId())
                .orElseThrow(AuthenticationFailedException::new);
    }

    private IssuedRefreshToken persistSession(RefreshSessionFamily family, long generation,
                                              LocalDateTime now, LocalDateTime expiresAt) {
        String rawToken = refreshTokenService.generate();
        RefreshSession session = new RefreshSession();
        session.setFamily(family);
        session.setGeneration(generation);
        session.setTokenHash(refreshTokenService.hash(rawToken));
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        sessionRepository.saveAndFlush(session);
        return new IssuedRefreshToken(rawToken, session.getId(), generation, family.getUtilisateur());
    }

    private boolean isReused(RefreshSession session) {
        return session.getUsedAt() != null || session.getReplacedById() != null;
    }

    private boolean isUsable(RefreshSession session, RefreshSessionFamily family, LocalDateTime now) {
        Utilisateur utilisateur = family.getUtilisateur();
        return session.getRevokedAt() == null
                && family.getRevokedAt() == null
                && session.getExpiresAt().isAfter(now)
                && family.getExpiresAt().isAfter(now)
                && utilisateur.getStatut() == StatutUtilisateur.ACTIF;
    }

    private void revokeFamily(RefreshSessionFamily family, String reason, boolean reuseDetected, LocalDateTime now) {
        if (family.getRevokedAt() != null) {
            return;
        }
        family.setRevokedAt(now);
        family.setRevocationReason(reason);
        recordEvent(family, SecurityEventType.REFRESH_FAMILY_REVOKED, now);
        if (reuseDetected) {
            recordEvent(family, SecurityEventType.REFRESH_TOKEN_REUSE_DETECTED, now);
        }
    }

    private void recordEvent(RefreshSessionFamily family, SecurityEventType type, LocalDateTime now) {
        SecurityEvent event = new SecurityEvent();
        event.setUtilisateur(family.getUtilisateur());
        event.setRefreshFamily(family);
        event.setEventType(type);
        event.setOccurredAt(now);
        securityEventRepository.save(event);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    public record IssuedRefreshToken(String rawToken, Long sessionId, long generation, Utilisateur utilisateur) {
    }
}
