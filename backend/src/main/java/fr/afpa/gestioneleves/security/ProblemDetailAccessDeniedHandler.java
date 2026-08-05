package fr.afpa.gestioneleves.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        boolean csrf = accessDeniedException instanceof CsrfException;
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        detail.setType(URI.create(csrf ? "urn:problem:csrf_invalid" : "urn:problem:access_denied"));
        detail.setTitle(csrf ? "Jeton CSRF invalide" : "Accès refusé");
        detail.setDetail(csrf ? "La protection CSRF est invalide ou manquante." : "Vous n’êtes pas autorisé à effectuer cette action.");
        detail.setProperty("code", csrf ? "csrf_invalid" : "access_denied");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
