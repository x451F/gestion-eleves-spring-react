package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record EleveRequest(
        @NotBlank @Size(max = 50) String numeroDossier,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotNull @Past LocalDate dateNaissance,
        @Email @Size(max = 180) String email,
        @Size(max = 30) String telephone,
        Boolean actif
) {
}
