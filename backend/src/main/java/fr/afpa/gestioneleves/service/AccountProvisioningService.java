package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.ProvisionGuardianAccountRequest;
import fr.afpa.gestioneleves.dto.request.ProvisionTeacherAccountRequest;
import fr.afpa.gestioneleves.entity.ActivationToken;
import fr.afpa.gestioneleves.entity.Enseignant;
import fr.afpa.gestioneleves.entity.Responsable;
import fr.afpa.gestioneleves.entity.SecurityEvent;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.repository.ActivationTokenRepository;
import fr.afpa.gestioneleves.repository.EnseignantRepository;
import fr.afpa.gestioneleves.repository.ResponsableRepository;
import fr.afpa.gestioneleves.repository.SecurityEventRepository;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.OpaqueTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AccountProvisioningService {
    private static final String RESEND_REASON = "RESEND";
    private final UtilisateurRepository userRepository;
    private final EnseignantRepository teacherRepository;
    private final ResponsableRepository guardianRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final SecurityEventRepository eventRepository;
    private final OpaqueTokenService tokenService;
    private final Clock clock;

    public AccountProvisioningService(UtilisateurRepository userRepository, EnseignantRepository teacherRepository,
                                      ResponsableRepository guardianRepository, ActivationTokenRepository activationTokenRepository,
                                      SecurityEventRepository eventRepository, OpaqueTokenService tokenService, Clock clock) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.guardianRepository = guardianRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Transactional
    public ProvisionedAccount provisionTeacher(ProvisionTeacherAccountRequest request) {
        String email = canonicalEmail(request.email());
        rejectExistingEmail(email);
        Enseignant profile = request.enseignantId() == null ? newTeacher(request, email) : existingTeacher(request.enseignantId());
        rejectLinkedProfile(profile.getUtilisateur());
        Utilisateur user = pendingAccount(email, Role.ENSEIGNANT);
        profile.setUtilisateur(user);
        teacherRepository.save(profile);
        String rawToken = createActivation(user, SecurityEventType.INVITATION_CREATED);
        return new ProvisionedAccount(user, profile.getId(), rawToken);
    }

    @Transactional
    public ProvisionedAccount provisionGuardian(ProvisionGuardianAccountRequest request) {
        String email = canonicalEmail(request.email());
        rejectExistingEmail(email);
        Responsable profile = request.responsableId() == null ? newGuardian(request, email) : existingGuardian(request.responsableId());
        rejectLinkedProfile(profile.getUtilisateur());
        Utilisateur user = pendingAccount(email, Role.RESPONSABLE);
        profile.setUtilisateur(user);
        guardianRepository.save(profile);
        String rawToken = createActivation(user, SecurityEventType.INVITATION_CREATED);
        return new ProvisionedAccount(user, profile.getId(), rawToken);
    }

    @Transactional
    public ProvisionedAccount bootstrapAdmin(String suppliedEmail) {
        String email = canonicalEmail(suppliedEmail);
        userRepository.lockBootstrapAdminCreation();
        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new BusinessRuleException("Un compte administrateur existe déjà.");
        }
        rejectExistingEmail(email);
        Utilisateur user = pendingAccount(email, Role.ADMIN);
        String rawToken = createActivation(user, SecurityEventType.INVITATION_CREATED);
        return new ProvisionedAccount(user, null, rawToken);
    }

    @Transactional
    public ProvisionedAccount resendActivation(Long userId) {
        Utilisateur user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable."));
        if (user.getStatut() != StatutUtilisateur.EN_ATTENTE_ACTIVATION) {
            throw new BusinessRuleException("Ce compte ne peut pas recevoir un nouveau lien d’activation.");
        }
        LocalDateTime now = now();
        activationTokenRepository.revokeUnusedForUser(user.getId(), now, RESEND_REASON);
        String rawToken = createActivation(user, SecurityEventType.INVITATION_RESENT);
        return new ProvisionedAccount(user, profileId(user), rawToken);
    }

    private Utilisateur pendingAccount(String email, Role role) {
        Utilisateur user = new Utilisateur();
        user.setEmailNormalise(email);
        user.setRole(role);
        user.setStatut(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
        return userRepository.save(user);
    }

    private String createActivation(Utilisateur user, SecurityEventType type) {
        String rawToken = tokenService.generate();
        ActivationToken token = new ActivationToken();
        token.setUtilisateur(user);
        token.setTokenHash(tokenService.hash(rawToken));
        token.setCreatedAt(now());
        token.setExpiresAt(now().plusHours(24));
        activationTokenRepository.save(token);
        record(user, type);
        return rawToken;
    }

    private Enseignant newTeacher(ProvisionTeacherAccountRequest request, String email) {
        required(request.matricule(), "Le matricule enseignant est requis.");
        required(request.nom(), "Le nom enseignant est requis.");
        required(request.prenom(), "Le prénom enseignant est requis.");
        if (teacherRepository.existsByMatriculeIgnoreCase(request.matricule()) || teacherRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Le profil enseignant existe déjà.");
        }
        Enseignant profile = new Enseignant();
        profile.setMatricule(request.matricule().trim().toUpperCase(Locale.ROOT));
        profile.setNom(request.nom().trim());
        profile.setPrenom(request.prenom().trim());
        profile.setEmail(email);
        profile.setActif(true);
        return teacherRepository.save(profile);
    }

    private Responsable newGuardian(ProvisionGuardianAccountRequest request, String email) {
        required(request.nom(), "Le nom responsable est requis.");
        required(request.prenom(), "Le prénom responsable est requis.");
        if (guardianRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Le profil responsable existe déjà.");
        }
        Responsable profile = new Responsable();
        profile.setNom(request.nom().trim());
        profile.setPrenom(request.prenom().trim());
        profile.setEmail(email);
        profile.setTelephone(request.telephone() == null || request.telephone().isBlank() ? null : request.telephone().trim());
        profile.setActif(true);
        return guardianRepository.save(profile);
    }

    private Enseignant existingTeacher(Long id) {
        return teacherRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Profil introuvable."));
    }

    private Responsable existingGuardian(Long id) {
        return guardianRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Profil introuvable."));
    }

    private void rejectExistingEmail(String email) {
        if (userRepository.findByEmailNormalise(email).isPresent()) {
            throw new DuplicateResourceException("Cette adresse e-mail est déjà utilisée.");
        }
    }

    private void rejectLinkedProfile(Utilisateur linkedUser) {
        if (linkedUser != null) {
            throw new BusinessRuleException("Ce profil ne peut pas être associé à un nouveau compte.");
        }
    }

    private String canonicalEmail(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleException("Une adresse e-mail valide est requise.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void required(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessRuleException(message);
    }

    private Long profileId(Utilisateur user) {
        if (user.getRole() == Role.ADMIN) return null;
        return user.getRole() == Role.ENSEIGNANT
                ? teacherRepository.findByUtilisateurId(user.getId()).map(Enseignant::getId).orElse(null)
                : guardianRepository.findByUtilisateurId(user.getId()).map(Responsable::getId).orElse(null);
    }

    private void record(Utilisateur user, SecurityEventType type) {
        SecurityEvent event = new SecurityEvent();
        event.setUtilisateur(user);
        event.setEventType(type);
        event.setOccurredAt(now());
        eventRepository.save(event);
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), clock.getZone()); }

    public record ProvisionedAccount(Utilisateur user, Long profileId, String rawActivationToken) {
    }
}
