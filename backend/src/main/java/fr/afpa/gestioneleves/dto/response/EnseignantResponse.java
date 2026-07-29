package fr.afpa.gestioneleves.dto.response;

public record EnseignantResponse(
        Long id, String matricule, String nom, String prenom, String email, Long utilisateurId, boolean actif
) {
}
