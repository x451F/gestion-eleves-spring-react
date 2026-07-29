package fr.afpa.gestioneleves.dto.response;

public record ResponsableResponse(
        Long id, String nom, String prenom, String email, String telephone, Long utilisateurId, boolean actif
) {
}
