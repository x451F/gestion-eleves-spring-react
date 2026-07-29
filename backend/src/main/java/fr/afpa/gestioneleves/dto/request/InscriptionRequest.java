package fr.afpa.gestioneleves.dto.request;

import fr.afpa.gestioneleves.enumtype.StatutInscription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record InscriptionRequest(
        @NotNull Long eleveId,
        @NotNull Long classeId,
        @NotNull @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "doit respecter le format YYYY-YYYY") String anneeScolaire,
        @NotNull LocalDate dateInscription,
        LocalDate dateFin,
        @NotNull StatutInscription statut
) {
}
