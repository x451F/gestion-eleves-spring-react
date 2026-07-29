package fr.afpa.gestioneleves.dto.response;

import java.math.BigDecimal;

public record MatiereResponse(Long id, String code, String nom, BigDecimal coefficientDefaut, boolean actif) {
}
