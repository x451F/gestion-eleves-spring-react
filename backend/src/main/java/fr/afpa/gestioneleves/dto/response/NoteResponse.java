package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NoteResponse(
        Long id, Long inscriptionId, Long enseignementId, String matiere,
        PeriodeBulletin periode, BigDecimal valeur, BigDecimal bareme, BigDecimal coefficient,
        LocalDate dateEvaluation, String libelle, String commentaire
) {
}
