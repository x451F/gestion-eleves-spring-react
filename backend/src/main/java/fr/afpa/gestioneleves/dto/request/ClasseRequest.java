package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClasseRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String niveau,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "doit respecter le format YYYY-YYYY") String anneeScolaire,
        Boolean actif
) {
}
