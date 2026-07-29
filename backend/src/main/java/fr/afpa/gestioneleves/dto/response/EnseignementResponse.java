package fr.afpa.gestioneleves.dto.response;

import java.math.BigDecimal;

public record EnseignementResponse(
        Long id, Long enseignantId, String enseignantNomComplet, Long matiereId, String matiereNom,
        Long classeId, String classeNom, String anneeScolaire, BigDecimal coefficientMatiere
) {
}
