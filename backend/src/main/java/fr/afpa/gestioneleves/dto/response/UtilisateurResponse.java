package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.Role;

import java.time.LocalDateTime;

public record UtilisateurResponse(
        Long id, String email, Role role, boolean actif, LocalDateTime createdAt, LocalDateTime lastLoginAt
) {
}
