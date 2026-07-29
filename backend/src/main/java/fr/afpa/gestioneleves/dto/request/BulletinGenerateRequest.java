package fr.afpa.gestioneleves.dto.request;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulletinGenerateRequest(
        @NotNull Long inscriptionId,
        @NotNull PeriodeBulletin periode,
        @Size(max = 1000) String appreciation
) {
}
