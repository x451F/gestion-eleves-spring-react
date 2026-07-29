package fr.afpa.gestioneleves.dto.request;

import fr.afpa.gestioneleves.enumtype.LienParente;
import jakarta.validation.constraints.NotNull;

public record AssociationResponsableRequest(
        @NotNull LienParente lienParente,
        boolean responsablePrincipal,
        boolean autoriteParentale,
        boolean contactUrgence
) {
}
