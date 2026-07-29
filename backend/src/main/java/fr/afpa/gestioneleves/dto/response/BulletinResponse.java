package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.enumtype.StatutBulletin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BulletinResponse(
        Long id, Long inscriptionId, String eleveNomComplet, String classeNom, String anneeScolaire,
        PeriodeBulletin periode, LocalDateTime dateGeneration, StatutBulletin statut,
        BigDecimal moyenneGenerale, String appreciation, List<Ligne> lignes
) {
    public record Ligne(String codeMatiere, String nomMatiere, BigDecimal moyenne,
                        BigDecimal coefficient, int nombreNotes) {
    }
}
