package fr.afpa.gestioneleves.security;

import fr.afpa.gestioneleves.entity.Utilisateur;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AccessTokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public AccessTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedAccessToken issue(Utilisateur utilisateur) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenLifetime());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(utilisateur.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", utilisateur.getRole().name())
                .claim("ver", utilisateur.getTokenVersion())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new IssuedAccessToken(token, properties.accessTokenLifetime().toSeconds());
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
