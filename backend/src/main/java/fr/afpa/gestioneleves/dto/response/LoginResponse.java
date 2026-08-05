package fr.afpa.gestioneleves.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
