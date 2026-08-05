package fr.afpa.gestioneleves.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.gestioneleves.entity.RefreshSession;
import fr.afpa.gestioneleves.entity.RefreshSessionFamily;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.repository.RefreshSessionFamilyRepository;
import fr.afpa.gestioneleves.repository.RefreshSessionRepository;
import fr.afpa.gestioneleves.repository.SecurityEventRepository;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.AuthenticationFailedException;
import fr.afpa.gestioneleves.security.RefreshTokenService;
import fr.afpa.gestioneleves.service.RefreshSessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RefreshSessionIntegrationTest {
    private static final String PASSWORD = "correct horse battery staple";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("refresh_sessions").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired RefreshSessionFamilyRepository familyRepository;
    @Autowired RefreshSessionRepository sessionRepository;
    @Autowired RefreshSessionService refreshSessionService;
    @Autowired RefreshTokenService refreshTokenService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired SecurityEventRepository securityEventRepository;

    @Test
    void loginIssuesDistinctHashOnlyRefreshFamiliesAndSecurelyScopedCookie() throws Exception {
        createUser("alice@example.fr");

        Login first = login("alice@example.fr");
        Login second = login("alice@example.fr");

        List<RefreshSessionFamily> families = familyRepository.findAll();
        List<RefreshSession> sessions = sessionRepository.findAll();
        assertThat(families).hasSize(2);
        assertThat(sessions).hasSize(2).allSatisfy(session -> {
            assertThat(session.getGeneration()).isZero();
            assertThat(session.getTokenHash()).matches("[0-9a-f]{64}");
            assertThat(session.getTokenHash()).isNotEqualTo(first.refreshCookie().getValue())
                    .isNotEqualTo(second.refreshCookie().getValue());
        });
        assertThat(first.accessToken()).doesNotContain(first.refreshCookie().getValue());
        assertThat(first.refreshCookie().isHttpOnly()).isTrue();
        assertThat(first.refreshCookie().getPath()).isEqualTo("/api/auth");
        assertThat(first.refreshCookie().getDomain()).isNull();
        assertThat(first.refreshCookie().getSecure()).isFalse();
        assertThat(first.cookieHeader()).contains("SameSite=Strict").contains("Max-Age=604800");
        assertThat(first.cookieHeader()).doesNotContain("access_token");
        assertThat(first.responseBody()).doesNotContain("refresh_token");
    }

    @Test
    void refreshRequiresCsrfAndRotatesWithinTheExistingFamily() throws Exception {
        createUser("alice@example.fr");
        Login login = login("alice@example.fr");

        mockMvc.perform(post("/api/auth/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("csrf_invalid"));
        Csrf mismatch = csrf();
        mockMvc.perform(post("/api/auth/refresh").cookie(login.refreshCookie()).cookie(mismatch.cookie())
                        .header(mismatch.headerName(), mismatch.token() + "mismatch"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("csrf_invalid"));
        mockMvc.perform(post("/api/auth/logout").cookie(login.refreshCookie()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("csrf_invalid"));
        Csrf csrf = csrf();
        var refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(login.refreshCookie())
                        .cookie(csrf.cookie()).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andReturn();

        Cookie rotatedCookie = refreshed.getResponse().getCookie("refresh_token");
        List<RefreshSession> sessions = sessionRepository.findAll();
        assertThat(sessions).hasSize(2);
        RefreshSession oldSession = sessions.stream().filter(session -> session.getGeneration() == 0).findFirst().orElseThrow();
        RefreshSession newSession = sessions.stream().filter(session -> session.getGeneration() == 1).findFirst().orElseThrow();
        assertThat(oldSession.getUsedAt()).isNotNull();
        assertThat(oldSession.getReplacedById()).isEqualTo(newSession.getId());
        assertThat(oldSession.getReplacedByGeneration()).isEqualTo(1);
        assertThat(oldSession.getFamily().getId()).isEqualTo(newSession.getFamily().getId());
        assertThat(newSession.getTokenHash()).isEqualTo(refreshTokenService.hash(rotatedCookie.getValue()))
                .isNotEqualTo(rotatedCookie.getValue());
        assertThat(login.accessToken()).isNotEqualTo(readAccessToken(refreshed.getResponse().getContentAsString()));
    }

    @Test
    void reusedTokenRevokesOnlyItsFamilyAndRecordsReplayEvidence() throws Exception {
        createUser("alice@example.fr");
        Login firstDevice = login("alice@example.fr");
        Login secondDevice = login("alice@example.fr");
        Cookie replacement = refresh(firstDevice.refreshCookie()).refreshCookie();

        String reuseFailure = refreshFailure(firstDevice.refreshCookie());
        assertThat(reuseFailure).contains("authentication_failed").doesNotContain("family").doesNotContain("hash");
        assertThat(familyFor(firstDevice.refreshCookie()).getRevokedAt()).isNotNull();
        assertThat(familyFor(secondDevice.refreshCookie()).getRevokedAt()).isNull();
        assertThat(securityEventRepository.findAll()).extracting(event -> event.getEventType())
                .contains(SecurityEventType.REFRESH_TOKEN_REUSE_DETECTED, SecurityEventType.REFRESH_FAMILY_REVOKED);

        assertThat(refreshFailure(replacement)).isEqualTo(reuseFailure);
        assertThat(refresh(secondDevice.refreshCookie()).accessToken()).isNotBlank();
        assertThat(sessionRepository.findAll()).hasSize(4);
    }

    @Test
    void logoutIsIdempotentAndLogoutAllInvalidatesAccessTokensAndAllFamilies() throws Exception {
        Utilisateur user = createUser("alice@example.fr");
        Login firstDevice = login("alice@example.fr");
        Login secondDevice = login("alice@example.fr");

        Csrf csrf = csrf();
        mockMvc.perform(post("/api/auth/logout").cookie(firstDevice.refreshCookie()).cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=;")));
        mockMvc.perform(post("/api/auth/logout").cookie(firstDevice.refreshCookie()).cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());
        assertThat(familyFor(firstDevice.refreshCookie()).getRevokedAt()).isNotNull();
        assertThat(familyFor(secondDevice.refreshCookie()).getRevokedAt()).isNull();

        mockMvc.perform(post("/api/auth/logout-all").header(HttpHeaders.AUTHORIZATION, "Bearer " + secondDevice.accessToken())
                        .cookie(secondDevice.refreshCookie()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("csrf_invalid"));
        Csrf logoutAllCsrf = csrf();
        mockMvc.perform(post("/api/auth/logout-all").header(HttpHeaders.AUTHORIZATION, "Bearer " + secondDevice.accessToken())
                        .cookie(secondDevice.refreshCookie()).cookie(logoutAllCsrf.cookie())
                        .header(logoutAllCsrf.headerName(), logoutAllCsrf.token()))
                .andExpect(status().isNoContent());
        assertThat(utilisateurRepository.findById(user.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
        assertThat(familyRepository.findAll()).allSatisfy(family -> assertThat(family.getRevokedAt()).isNotNull());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + secondDevice.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshHasExactlyOneReplacementAndTheLoserTriggersFamilyRevocation() throws Exception {
        Utilisateur user = createUser("alice@example.fr");
        String rawToken = refreshSessionService.createFamily(user).rawToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> {
                ready.countDown();
                start.await();
                try {
                    refreshSessionService.rotate(rawToken);
                    return true;
                } catch (AuthenticationFailedException ex) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);
            ready.await();
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
        assertThat(sessionRepository.findAll()).hasSize(2);
        assertThat(familyRepository.findAll()).allSatisfy(family -> assertThat(family.getRevokedAt()).isNotNull());
    }

    @Test
    void exactCredentialedCorsAllowsOnlyConfiguredOriginAndCsrfHeader() throws Exception {
        mockMvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("X-XSRF-TOKEN")));
        mockMvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://unconfigured.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void refreshFailuresRemainExternallyGenericForMissingUnknownExpiredAndInactiveSessions() throws Exception {
        Utilisateur user = createUser("alice@example.fr");
        Login login = login("alice@example.fr");
        String missing = refreshFailureWithoutCookie();
        String unknown = refreshFailure(new Cookie("refresh_token", "a".repeat(43)));

        RefreshSession session = sessionRepository.findByTokenHash(refreshTokenService.hash(login.refreshCookie().getValue())).orElseThrow();
        jdbcTemplate.update("UPDATE refresh_session SET created_at = now() - interval '2 days', "
                + "expires_at = now() - interval '1 day' WHERE id = ?", session.getId());
        String expired = refreshFailure(login.refreshCookie());

        Login anotherLogin = login("alice@example.fr");
        Utilisateur currentUser = utilisateurRepository.findById(user.getId()).orElseThrow();
        currentUser.setStatut(StatutUtilisateur.DESACTIVE);
        utilisateurRepository.saveAndFlush(currentUser);
        String inactive = refreshFailure(anotherLogin.refreshCookie());

        assertThat(missing).isEqualTo(unknown).isEqualTo(expired).isEqualTo(inactive);
    }

    private Login login(String email) throws Exception {
        Csrf csrf = csrf();
        var result = mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        return new Login(readAccessToken(body), result.getResponse().getCookie("refresh_token"),
                result.getResponse().getHeader(HttpHeaders.SET_COOKIE), body);
    }

    private Login refresh(Cookie refreshCookie) throws Exception {
        Csrf csrf = csrf();
        var result = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie).cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        return new Login(readAccessToken(body), result.getResponse().getCookie("refresh_token"),
                result.getResponse().getHeader(HttpHeaders.SET_COOKIE), body);
    }

    private String refreshFailure(Cookie refreshCookie) throws Exception {
        Csrf csrf = csrf();
        return mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie).cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=;")))
                .andReturn().getResponse().getContentAsString();
    }

    private String refreshFailureWithoutCookie() throws Exception {
        Csrf csrf = csrf();
        return mockMvc.perform(post("/api/auth/refresh").cookie(csrf.cookie()).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=;")))
                .andReturn().getResponse().getContentAsString();
    }

    private Csrf csrf() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(cookie, body.get("headerName").asText(), body.get("token").asText());
    }

    private RefreshSessionFamily familyFor(Cookie cookie) {
        Long familyId = sessionRepository.findByTokenHash(refreshTokenService.hash(cookie.getValue()))
                .orElseThrow().getFamily().getId();
        return familyRepository.findById(familyId).orElseThrow();
    }

    private String readAccessToken(String body) throws Exception {
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private Utilisateur createUser(String email) {
        Utilisateur user = new Utilisateur();
        user.setEmailNormalise(email);
        user.setRole(Role.ADMIN);
        user.setStatut(StatutUtilisateur.ACTIF);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setEmailVerifieAt(LocalDateTime.now());
        return utilisateurRepository.saveAndFlush(user);
    }

    private record Csrf(Cookie cookie, String headerName, String token) {
    }

    private record Login(String accessToken, Cookie refreshCookie, String cookieHeader, String responseBody) {
    }
}
