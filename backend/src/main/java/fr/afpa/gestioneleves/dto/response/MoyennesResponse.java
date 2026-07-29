package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;

import java.math.BigDecimal;
import java.util.List;

public record MoyennesResponse(Long inscriptionId, PeriodeBulletin periode,
                               BigDecimal moyenneGenerale, List<Matiere> matieres) {
    public record Matiere(Long matiereId, String nom, BigDecimal moyenne, BigDecimal coefficient, int nombreNotes) {
    }
}
