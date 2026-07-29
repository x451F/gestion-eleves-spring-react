package fr.afpa.gestioneleves.dto.response;

public record ClasseResponse(Long id, String code, String nom, String niveau, String anneeScolaire, boolean actif) {
}
