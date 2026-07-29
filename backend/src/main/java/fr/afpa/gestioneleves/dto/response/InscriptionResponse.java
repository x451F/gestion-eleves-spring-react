package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.StatutInscription;

import java.time.LocalDate;

public record InscriptionResponse(
        Long id, Long eleveId, String eleveNomComplet, Long classeId, String classeNom,
        String anneeScolaire, LocalDate dateInscription, LocalDate dateFin, StatutInscription statut
) {
}
