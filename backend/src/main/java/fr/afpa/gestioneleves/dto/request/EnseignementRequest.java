package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record EnseignementRequest(
        @NotNull Long enseignantId,
        @NotNull Long matiereId,
        @NotNull Long classeId,
        @NotNull @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "doit respecter le format YYYY-YYYY") String anneeScolaire,
        @NotNull @DecimalMin(value = "0.01") BigDecimal coefficientMatiere
) {
}
