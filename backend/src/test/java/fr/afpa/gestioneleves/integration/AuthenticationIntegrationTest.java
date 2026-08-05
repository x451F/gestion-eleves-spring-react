package fr.afpa.gestioneleves.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.PasswordPolicy;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthenticationIntegrationTest {
    private static final String PASSWORD = "correct horse battery staple";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("authentication").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PasswordPolicy passwordPolicy;
    @Autowired JwtEncoder jwtEncoder;

    @Test
    void activeAccountCanLoginReceiveOnlyRequiredClaimsAndAccessCurrentUserWithoutSession() throws Exception {
        Utilisateur user = createUser("alice@example.fr", Role.ADMIN, StatutUtilisateur.ACTIF, 0, PASSWORD);

        Csrf csrf = csrf();
        var login = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.fr\",\"password\":\"correct horse battery staple\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andReturn();

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        Map<String, Object> claims = SignedJWT.parse(token).getJWTClaimsSet().getClaims();
        assertThat(claims).containsOnlyKeys("sub", "role", "ver", "jti", "iss", "iat", "exp");
        assertThat(claims).containsEntry("sub", user.getId().toString())
                .containsEntry("role", "ADMIN").containsEntry("ver", 0L)
                .containsEntry("iss", "gestion-eleves-api-test");
        assertThat(Duration.between(((Date) claims.get("iat")).toInstant(), ((Date) claims.get("exp")).toInstant()))
                .isEqualTo(Duration.ofMinutes(15));
        assertThat(claims).doesNotContainKeys("email", "password", "passwordHash", "profile");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("alice@example.fr"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIF"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.tokenVersion").doesNotExist())
                .andExpect(jsonPath("$.refreshSession").doesNotExist());
        assertThat(login.getRequest().getSession(false)).isNull();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginNormalizesEmailAndAllCredentialFailuresShareTheSameGenericProblemDetail() throws Exception {
        createUser("alice@example.fr", Role.ADMIN, StatutUtilisateur.ACTIF, 0, PASSWORD);
        createUser("pending@example.fr", Role.ADMIN, StatutUtilisateur.EN_ATTENTE_ACTIVATION, 0, null);
        createUser("disabled@example.fr", Role.ADMIN, StatutUtilisateur.DESACTIVE, 0, PASSWORD);

        Csrf csrf = csrf();
        mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  ALICE@EXAMPLE.FR \",\"password\":\"correct horse battery staple\"}"))
                .andExpect(status().isOk());

        String unknown = failedLogin("unknown@example.fr", PASSWORD);
        String wrong = failedLogin("alice@example.fr", "wrong password");
        String pending = failedLogin("pending@example.fr", PASSWORD);
        String disabled = failedLogin("disabled@example.fr", PASSWORD);
        assertThat(unknown).isEqualTo(wrong).isEqualTo(pending).isEqualTo(disabled);
        assertThat(unknown).contains("authentication_failed").doesNotContain("Set-Cookie");
    }

    @Test
    void passwordEncoderUsesBcryptTwelveAndSafelySupportsOnlyLegacyBcryptHashes() {
        String encoded = passwordEncoder.encode(PASSWORD);
        assertThat(encoded).startsWith("{bcrypt}$2");
        assertThat(passwordEncoder.matches(PASSWORD, encoded)).isTrue();
        String legacyBcrypt = new BCryptPasswordEncoder(12).encode(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, legacyBcrypt)).isTrue();
        assertThat(matchesWithoutThrowing(PASSWORD, PASSWORD)).isFalse();

        assertThat(passwordPolicy.isValid("a b c d e f g h i j k l")).isTrue();
        assertThat(passwordPolicy.isValid("long passphrase with spaces")).isTrue();
        assertThat(passwordPolicy.isValid("a".repeat(11))).isFalse();
        assertThat(passwordPolicy.isValid("a".repeat(65))).isFalse();
        assertThat(passwordPolicy.isValid("simple passphrase")).isTrue();
    }

    @Test
    void missingAndInvalidBearerTokensAreRejectedWithFrenchProblemDetails() throws Exception {
        createUser("alice@example.fr", Role.ADMIN, StatutUtilisateur.ACTIF, 0, PASSWORD);
        for (String token : new String[]{null, "not.a.jwt", "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0."}) {
            var request = get("/api/auth/me");
            if (token != null) request.header("Authorization", "Bearer " + token);
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.code").value("invalid_access_token"))
                    .andExpect(jsonPath("$.title").value("Jeton d’accès invalide"));
        }
    }

    @Test
    void signatureIssuerAlgorithmAndClaimValidationAreStrict() throws Exception {
        Utilisateur user = createUser("alice@example.fr", Role.ADMIN, StatutUtilisateur.ACTIF, 0, PASSWORD);
        String valid = signedToken(user.getId(), "ADMIN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true);
        assertAccepted(valid);
        assertRejected(valid.substring(0, valid.length() - 1) + (valid.endsWith("A") ? "B" : "A"));
        assertRejected(signedToken(user.getId(), "ADMIN", 0, "other-issuer", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true));
        assertRejected("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0.");
        assertRejected(signedToken(user.getId(), "ADMIN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(901), Instant.now().minusSeconds(1), true));
        assertRejected(signedToken(user.getId(), "UNKNOWN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true));
        assertRejected(signedToken(-1, "ADMIN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true));
        assertRejected(signedToken(user.getId(), "ADMIN", -1, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true));
        assertRejected(signedToken(user.getId(), "ADMIN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), false));
    }

    @Test
    void databaseStatusVersionRoleAndExistenceAreAuthoritativeForEveryRequest() throws Exception {
        Utilisateur user = createUser("alice@example.fr", Role.ADMIN, StatutUtilisateur.ACTIF, 0, PASSWORD);
        String token = signedToken(user.getId(), "ADMIN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true);
        assertAccepted(token);

        user.setTokenVersion(1);
        user = utilisateurRepository.saveAndFlush(user);
        assertRejected(token);
        String versionOne = signedToken(user.getId(), "ADMIN", 1, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true);
        assertAccepted(versionOne);
        assertRejected(signedToken(user.getId(), "ENSEIGNANT", 1, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true));
        assertRejected(signedToken(user.getId() + 1000, "ADMIN", 0, "gestion-eleves-api-test", Instant.now().minusSeconds(5), Instant.now().plusSeconds(900), true));

        user.setStatut(StatutUtilisateur.DESACTIVE);
        user = utilisateurRepository.saveAndFlush(user);
        assertRejected(versionOne);

        user.setStatut(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
        user.setPasswordHash(null);
        user.setEmailVerifieAt(null);
        utilisateurRepository.saveAndFlush(user);
        assertRejected(versionOne);
    }

    private Utilisateur createUser(String email, Role role, StatutUtilisateur status, long tokenVersion, String password) {
        Utilisateur user = new Utilisateur();
        user.setEmailNormalise(email);
        user.setRole(role);
        user.setStatut(status);
        user.setTokenVersion(tokenVersion);
        if (password != null) {
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setEmailVerifieAt(LocalDateTime.now());
        }
        return utilisateurRepository.saveAndFlush(user);
    }

    private String failedLogin(String email, String password) throws Exception {
        Csrf csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("authentication_failed"))
                .andReturn().getResponse().getContentAsString();
    }

    private Csrf csrf() throws Exception {
        var bootstrap = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();
        Cookie cookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        String token = objectMapper.readTree(bootstrap.getResponse().getContentAsString()).get("token").asText();
        return new Csrf(cookie, "X-XSRF-TOKEN", token);
    }

    private record Csrf(Cookie cookie, String headerName, String token) {
    }

    private boolean matchesWithoutThrowing(String rawPassword, String storedHash) {
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String signedToken(long subject, String role, long version, String issuer,
                               Instant issuedAt, Instant expiresAt, boolean includeJti) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject(Long.toString(subject)).issuer(issuer)
                .issuedAt(issuedAt).expiresAt(expiresAt).claim("role", role).claim("ver", version);
        if (includeJti) claims.id(UUID.randomUUID().toString());
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    private void assertAccepted(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    private void assertRejected(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
}
