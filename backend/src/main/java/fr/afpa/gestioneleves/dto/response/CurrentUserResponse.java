package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;

public record CurrentUserResponse(Long id, String email, Role role, StatutUtilisateur status) {
}
