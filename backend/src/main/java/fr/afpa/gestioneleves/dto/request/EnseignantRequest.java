package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnseignantRequest(
        @NotBlank @Size(max = 50) String matricule,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 180) String email,
        Boolean actif
) {
}
