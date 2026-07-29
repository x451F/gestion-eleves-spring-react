package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MatiereRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String nom,
        @NotNull @DecimalMin(value = "0.01") BigDecimal coefficientDefaut,
        Boolean actif
) {
}
