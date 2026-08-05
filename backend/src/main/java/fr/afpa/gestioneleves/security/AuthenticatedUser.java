package fr.afpa.gestioneleves.security;

import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;

import java.security.Principal;

public record AuthenticatedUser(Long id, String email, Role role, StatutUtilisateur status) implements Principal {
    @Override
    public String getName() {
        return id.toString();
    }
}
