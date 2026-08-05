package fr.afpa.gestioneleves.security;

import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UtilisateurRepository utilisateurRepository;

    public AccessJwtAuthenticationConverter(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new InvalidBearerTokenException("Invalid access token");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new InvalidBearerTokenException("Invalid access token"));
        Role claimedRole;
        try {
            claimedRole = Role.valueOf(jwt.getClaimAsString("role"));
        } catch (RuntimeException ex) {
            throw new InvalidBearerTokenException("Invalid access token");
        }
        Number claimedVersion = jwt.getClaim("ver");
        if (utilisateur.getStatut() != StatutUtilisateur.ACTIF
                || utilisateur.getTokenVersion() != claimedVersion.longValue()
                || utilisateur.getRole() != claimedRole) {
            throw new InvalidBearerTokenException("Invalid access token");
        }

        AuthenticatedUser principal = new AuthenticatedUser(utilisateur.getId(), utilisateur.getEmailNormalise(),
                utilisateur.getRole(), utilisateur.getStatut());
        return new AccessJwtAuthenticationToken(principal, jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())));
    }
}
