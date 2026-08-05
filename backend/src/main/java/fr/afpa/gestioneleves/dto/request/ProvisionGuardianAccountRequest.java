package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionGuardianAccountRequest(
        Long responsableId,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 100) String nom,
        @Size(max = 100) String prenom,
        @Size(max = 30) String telephone
) {
}
