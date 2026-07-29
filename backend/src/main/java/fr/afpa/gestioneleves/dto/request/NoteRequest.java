package fr.afpa.gestioneleves.dto.request;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NoteRequest(
        @NotNull Long inscriptionId,
        @NotNull Long enseignementId,
        @NotNull PeriodeBulletin periode,
        @NotNull @DecimalMin("0.00") @DecimalMax("20.00") BigDecimal valeur,
        @NotNull @DecimalMin("20.00") @DecimalMax("20.00") BigDecimal bareme,
        @NotNull @DecimalMin(value = "0.01") BigDecimal coefficient,
        @NotNull @PastOrPresent LocalDate dateEvaluation,
        @Size(max = 150) String libelle,
        @Size(max = 500) String commentaire
) {
}
