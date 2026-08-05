package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionTeacherAccountRequest(
        Long enseignantId,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 50) String matricule,
        @Size(max = 100) String nom,
        @Size(max = 100) String prenom
) {
}
