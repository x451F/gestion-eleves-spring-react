package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAdministrationService {
    private final AccessPolicyService access;
    private final UtilisateurRepository users;
    private final RefreshSessionService refreshSessions;

    public AccountAdministrationService(AccessPolicyService access, UtilisateurRepository users,
                                        RefreshSessionService refreshSessions) {
        this.access = access; this.users = users; this.refreshSessions = refreshSessions;
    }

    @Transactional
    public void deactivate(AuthenticatedUser actor, Long targetId) {
        access.admin(actor);
        Utilisateur target = users.findByIdForUpdate(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable."));
        if (target.getStatut() == StatutUtilisateur.DESACTIVE) return;
        if (target.getRole() == Role.ADMIN) {
            long activeAdmins = users.findByRoleAndStatutForUpdate(Role.ADMIN, StatutUtilisateur.ACTIF).size();
            if (target.getStatut() == StatutUtilisateur.ACTIF && activeAdmins <= 1) {
                throw new BusinessRuleException("Le dernier compte administrateur actif ne peut pas être désactivé.");
            }
        }
        target.setStatut(StatutUtilisateur.DESACTIVE);
        refreshSessions.revokeAllForUser(target.getId());
    }
}
