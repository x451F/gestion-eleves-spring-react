package fr.afpa.gestioneleves.dto.response;

import fr.afpa.gestioneleves.enumtype.LienParente;

public record EleveResponsableResponse(
        Long id, Long eleveId, String eleveNomComplet, Long responsableId, String responsableNomComplet,
        LienParente lienParente, boolean responsablePrincipal, boolean autoriteParentale, boolean contactUrgence
) {
}
