package fr.afpa.gestioneleves.dto.response;

public record PhotoMetadataResponse(Long eleveId, String typeMime, long taille, String url) {
}
