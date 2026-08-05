package fr.afpa.gestioneleves.dto.request;

public record ResetPasswordRequest(String token, String password) {
}
