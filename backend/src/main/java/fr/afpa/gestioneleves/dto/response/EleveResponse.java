package fr.afpa.gestioneleves.dto.response;

import java.time.LocalDate;

public record EleveResponse(
        Long id, String numeroDossier, String nom, String prenom, LocalDate dateNaissance,
        String email, String telephone, boolean actif, boolean photoDisponible
) {
}
