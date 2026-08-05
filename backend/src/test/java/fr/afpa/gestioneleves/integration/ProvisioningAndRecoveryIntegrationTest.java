package fr.afpa.gestioneleves.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.gestioneleves.entity.ActivationToken;
import fr.afpa.gestioneleves.entity.Enseignant;
import fr.afpa.gestioneleves.entity.PasswordResetToken;
import fr.afpa.gestioneleves.entity.RefreshSessionFamily;
import fr.afpa.gestioneleves.entity.Responsable;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import fr.afpa.gestioneleves.repository.ActivationTokenRepository;
import fr.afpa.gestioneleves.repository.EnseignantRepository;
import fr.afpa.gestioneleves.repository.PasswordResetTokenRepository;
import fr.afpa.gestioneleves.repository.RefreshSessionFamilyRepository;
import fr.afpa.gestioneleves.repository.ResponsableRepository;
import fr.afpa.gestioneleves.repository.SecurityEventRepository;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import fr.afpa.gestioneleves.security.AccessTokenService;
import fr.afpa.gestioneleves.security.OpaqueTokenService;
import fr.afpa.gestioneleves.service.AccountProvisioningService;
import fr.afpa.gestioneleves.service.AccountRecoveryService;
import fr.afpa.gestioneleves.service.RefreshSessionService;
import fr.afpa.gestioneleves.dto.request.ProvisionTeacherAccountRequest;
import fr.afpa.gestioneleves.GestionElevesApplication;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
class ProvisioningAndRecoveryIntegrationTest {
    private static final String PASSWORD = "correct horse battery staple";
    private static final Pattern TOKEN = Pattern.compile("[?&]token=([^\\s]+)");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("provisioning").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository userRepository;
    @Autowired EnseignantRepository teacherRepository;
    @Autowired ResponsableRepository guardianRepository;
    @Autowired ActivationTokenRepository activationTokenRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired RefreshSessionFamilyRepository familyRepository;
    @Autowired SecurityEventRepository eventRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AccessTokenService accessTokenService;
    @Autowired OpaqueTokenService tokenService;
    @Autowired AccountProvisioningService provisioningService;
    @Autowired AccountRecoveryService recoveryService;
    @Autowired RefreshSessionService refreshSessionService;
    @Autowired JdbcTemplate jdbcTemplate;
    @MockBean JavaMailSender mailSender;

    private final List<SimpleMailMessage> sentMessages = new java.util.concurrent.CopyOnWriteArrayList<>();

    @BeforeEach
    void captureMail() {
        sentMessages.clear();
        doAnswer(invocation -> {
            sentMessages.add(new SimpleMailMessage(invocation.getArgument(0)));
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void normalStartupDoesNotSeedAdminAndBootstrapCreatesOnlyOnePendingHashOnlyAdmin() {
        assertThat(userRepository.findAll()).isEmpty();
        AccountProvisioningService.ProvisionedAccount admin = provisioningService.bootstrapAdmin(" First.Admin@Example.Fr ");
        Utilisateur stored = userRepository.findById(admin.user().getId()).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(Role.ADMIN);
        assertThat(stored.getStatut()).isEqualTo(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
        assertThat(stored.getPasswordHash()).isNull();
        ActivationToken token = activationTokenRepository.findAll().getFirst();
        assertThat(token.getTokenHash()).isEqualTo(tokenService.hash(admin.rawActivationToken()))
                .isNotEqualTo(admin.rawActivationToken()).matches("[0-9a-f]{64}");
        assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt().plusHours(23));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> provisioningService.bootstrapAdmin("second@example.fr"))
                .hasMessageContaining("administrateur existe déjà");
    }

    @Test
    void explicitCommandCreatesPendingAdminButBootstrapProfileAndEmailNeverSeedAWebApplication() {
        try (ConfigurableApplicationContext command = GestionElevesApplication.bootstrapCommandApplication().run(
                "--spring.profiles.active=test",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--app.bootstrap-admin.email=command@example.fr",
                "--spring.mail.properties.mail.smtp.connectiontimeout=500")) {
            Utilisateur admin = userRepository.findByEmailNormalise("command@example.fr").orElseThrow();
            assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
            assertThat(admin.getStatut()).isEqualTo(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
            assertThat(admin.getPasswordHash()).isNull();
            assertThat(activationTokenRepository.findAll()).hasSize(1);
        }

        jdbcTemplate.update("truncate table security_event, activation_token, utilisateur restart identity cascade");
        SpringApplication webApplication = new SpringApplication(GestionElevesApplication.class);
        webApplication.setWebApplicationType(WebApplicationType.SERVLET);
        webApplication.setAdditionalProfiles("test", "bootstrap");
        try (ConfigurableApplicationContext ignored = webApplication.run(
                "--server.port=0",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--app.bootstrap-admin.email=must-not-run@example.fr")) {
            assertThat(userRepository.findAll()).isEmpty();
            assertThat(activationTokenRepository.findAll()).isEmpty();
        }
    }

    @Test
    void concurrentBootstrapCreatesAtMostOneAdminAndOneActivationToken() throws Exception {
        assertThat(concurrentAttempts(
                () -> provisioningService.bootstrapAdmin("first@example.fr"),
                () -> provisioningService.bootstrapAdmin("second@example.fr")))
                .containsExactlyInAnyOrder(true, false);
        assertThat(userRepository.findAll()).singleElement().satisfies(user -> {
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
            assertThat(user.getStatut()).isEqualTo(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
            assertThat(user.getPasswordHash()).isNull();
        });
        assertThat(activationTokenRepository.findAll()).singleElement();
    }

    @Test
    void adminProvisioningCreatesMatchingProfileAndOnlyAdminCanUseTheEndpoints() throws Exception {
        String adminToken = accessToken(createActive("admin@example.fr", Role.ADMIN));
        mockMvc.perform(post("/api/admin/accounts/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Teacher@Example.Fr\",\"matricule\":\"T-001\",\"nom\":\"Dupont\",\"prenom\":\"Alice\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("ENSEIGNANT"))
                .andExpect(jsonPath("$.status").value("EN_ATTENTE_ACTIVATION")).andExpect(jsonPath("$.mailDelivered").value(true));
        Utilisateur teacher = userRepository.findByEmailNormalise("teacher@example.fr").orElseThrow();
        assertThat(teacher.getPasswordHash()).isNull();
        assertThat(teacherRepository.findByUtilisateurId(teacher.getId())).isPresent();
        assertThat(sentMessages).singleElement().satisfies(message -> {
            assertThat(message.getSubject()).isEqualTo("Activation de votre compte");
            assertThat(message.getText()).contains("Votre compte a été créé", "http://localhost:5173/activation?token=");
        });
        mockMvc.perform(post("/api/admin/accounts/guardians").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"guardian@example.fr\",\"nom\":\"Martin\",\"prenom\":\"Lea\"}"))
                .andExpect(status().isUnauthorized());
        AccountProvisioningService.ProvisionedAccount anotherTeacher = provisioningService.provisionTeacher(
                new ProvisionTeacherAccountRequest(null, "another@example.fr", "T-002", "Durand", "Paul"));
        recoveryService.activate(anotherTeacher.rawActivationToken(), PASSWORD);
        String teacherToken = accessToken(userRepository.findById(anotherTeacher.user().getId()).orElseThrow());
        mockMvc.perform(post("/api/admin/accounts/guardians").header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"guardian@example.fr\",\"nom\":\"Martin\",\"prenom\":\"Lea\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("access_denied"));
    }

    @Test
    void provisioningFailureToDeliverDoesNotRollbackAndResendRevokesThePreviousToken() throws Exception {
        doThrow(new MailSendException("simulated SMTP failure")).when(mailSender).send(any(SimpleMailMessage.class));
        String adminToken = accessToken(createActive("admin@example.fr", Role.ADMIN));
        var created = mockMvc.perform(post("/api/admin/accounts/guardians").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"guardian@example.fr\",\"nom\":\"Martin\",\"prenom\":\"Lea\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mailDelivered").value(false)).andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        ActivationToken first = activationTokenRepository.findAll().getFirst();
        assertThat(userRepository.findById(id)).isPresent();
        assertThat(eventRepository.findAll()).extracting(event -> event.getEventType()).contains(SecurityEventType.EMAIL_DELIVERY_FAILED);
        mockMvc.perform(post("/api/admin/accounts/{id}/resend-activation", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mailDelivered").value(false));
        assertThat(activationTokenRepository.findById(first.getId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(activationTokenRepository.findAll()).hasSize(2);
    }

    @Test
    void concurrentTeacherProfileProvisioningLocksTheProfileBeforeCreatingAnAccountOrMail() throws Exception {
        Enseignant profile = createUnlinkedTeacher("T-LOCK", "teacher-profile@example.fr");
        String adminToken = accessToken(createActive("admin@example.fr", Role.ADMIN));
        List<Integer> outcomes = concurrentHttpAttempts(
                () -> provisionExistingTeacher(profile.getId(), "teacher-one@example.fr", adminToken),
                () -> provisionExistingTeacher(profile.getId(), "teacher-two@example.fr", adminToken));

        assertThat(outcomes).containsExactlyInAnyOrder(201, 400);
        Utilisateur linked = teacherRepository.findById(profile.getId()).orElseThrow().getUtilisateur();
        assertThat(linked).isNotNull();
        assertThat(userRepository.findAll()).filteredOn(user -> user.getRole() == Role.ENSEIGNANT).singleElement()
                .extracting(Utilisateur::getId).isEqualTo(linked.getId());
        assertThat(activationTokenRepository.findAll()).singleElement().satisfies(token ->
                assertThat(token.getUtilisateur().getId()).isEqualTo(linked.getId()));
        assertThat(sentMessages).hasSize(1);
    }

    @Test
    void concurrentGuardianProfileProvisioningLocksTheProfileBeforeCreatingAnAccountOrMail() throws Exception {
        Responsable profile = createUnlinkedGuardian("guardian-profile@example.fr");
        String adminToken = accessToken(createActive("admin@example.fr", Role.ADMIN));
        List<Integer> outcomes = concurrentHttpAttempts(
                () -> provisionExistingGuardian(profile.getId(), "guardian-one@example.fr", adminToken),
                () -> provisionExistingGuardian(profile.getId(), "guardian-two@example.fr", adminToken));

        assertThat(outcomes).containsExactlyInAnyOrder(201, 400);
        Utilisateur linked = guardianRepository.findById(profile.getId()).orElseThrow().getUtilisateur();
        assertThat(linked).isNotNull();
        assertThat(userRepository.findAll()).filteredOn(user -> user.getRole() == Role.RESPONSABLE).singleElement()
                .extracting(Utilisateur::getId).isEqualTo(linked.getId());
        assertThat(activationTokenRepository.findAll()).singleElement().satisfies(token ->
                assertThat(token.getUtilisateur().getId()).isEqualTo(linked.getId()));
        assertThat(sentMessages).hasSize(1);
    }

    @Test
    void activationIsSingleUseStoresBcryptTwelveAndAllowsLoginOnlyAfterward() throws Exception {
        AccountProvisioningService.ProvisionedAccount account = provisioningService.bootstrapAdmin("admin@example.fr");
        String raw = account.rawActivationToken();
        failedLogin("admin@example.fr");
        mockMvc.perform(post("/api/auth/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", raw, "password", PASSWORD))))
                .andExpect(status().isNoContent());
        Utilisateur activated = userRepository.findById(account.user().getId()).orElseThrow();
        assertThat(activated.getStatut()).isEqualTo(StatutUtilisateur.ACTIF);
        assertThat(activated.getPasswordHash()).startsWith("{bcrypt}$2").doesNotContain(raw);
        assertThat(activationTokenRepository.findAll().getFirst().getUsedAt()).isNotNull();
        Csrf csrf = csrf();
        mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("email", "admin@example.fr", "password", PASSWORD))))
                .andExpect(status().isOk());
        String failure = mockMvc.perform(post("/api/auth/activate").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", raw, "password", PASSWORD))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_account_token"))
                .andReturn().getResponse().getContentAsString();
        assertThat(failure).doesNotContain("used", "admin@example.fr", raw);
    }

    @Test
    void concurrentActivationHasExactlyOneSuccess() throws Exception {
        AccountProvisioningService.ProvisionedAccount account = provisioningService.bootstrapAdmin("admin@example.fr");
        assertThat(concurrentAttempts(() -> recoveryService.activate(account.rawActivationToken(), PASSWORD)))
                .containsExactlyInAnyOrder(true, false);
        assertThat(userRepository.findById(account.user().getId()).orElseThrow().getStatut()).isEqualTo(StatutUtilisateur.ACTIF);
    }

    @Test
    void forgotPasswordIsNeutralAndResetRevokesFamiliesAndAccessTokens() throws Exception {
        Utilisateur active = createActive("active@example.fr", Role.ADMIN);
        Utilisateur pending = createPending("pending@example.fr", Role.ADMIN);
        Utilisateur disabled = createActive("disabled@example.fr", Role.ADMIN);
        disabled.setStatut(StatutUtilisateur.DESACTIVE);
        userRepository.saveAndFlush(disabled);
        String accessBefore = accessToken(active);
        refreshSessionService.createFamily(active);
        String known = forgot("active@example.fr");
        String unknown = forgot("unknown@example.fr");
        String pendingResponse = forgot(pending.getEmailNormalise());
        String disabledResponse = forgot(disabled.getEmailNormalise());
        assertThat(known).isEqualTo(unknown).isEqualTo(pendingResponse).isEqualTo(disabledResponse);
        assertThat(resetTokenRepository.findAll()).singleElement().satisfies(token -> assertThat(token.getTokenHash()).matches("[0-9a-f]{64}"));
        String raw = rawToken(sentMessages.getFirst());
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", raw, "password", "another valid passphrase"))))
                .andExpect(status().isNoContent()).andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=;")));
        Utilisateur changed = userRepository.findById(active.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("another valid passphrase", changed.getPasswordHash())).isTrue();
        assertThat(changed.getTokenVersion()).isEqualTo(1);
        assertThat(familyRepository.findAll()).extracting(RefreshSessionFamily::getRevokedAt).allMatch(value -> value != null);
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessBefore)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", raw, "password", "another valid passphrase"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_account_token"));
    }

    @Test
    void concurrentResetHasExactlyOneSuccessAndExpiredTokensFailWithoutSleeping() throws Exception {
        Utilisateur active = createActive("active@example.fr", Role.ADMIN);
        AccountRecoveryService.IssuedReset issued = recoveryService.requestPasswordReset(active.getEmailNormalise()).orElseThrow();
        assertThat(concurrentAttempts(() -> recoveryService.resetPassword(issued.rawToken(), "another valid passphrase")))
                .containsExactlyInAnyOrder(true, false);
        AccountRecoveryService.IssuedReset expired = recoveryService.requestPasswordReset(active.getEmailNormalise()).orElseThrow();
        PasswordResetToken token = resetTokenRepository.findByTokenHash(tokenService.hash(expired.rawToken())).orElseThrow();
        jdbcTemplate.update("update password_reset_token set expires_at = timestamp '2000-01-01 00:00:00' where id = ?", token.getId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> recoveryService.resetPassword(expired.rawToken(), "another valid passphrase"))
                .isInstanceOf(fr.afpa.gestioneleves.exception.InvalidAccountTokenException.class);
    }

    private Enseignant createUnlinkedTeacher(String matricule, String email) {
        Enseignant teacher = new Enseignant();
        teacher.setMatricule(matricule);
        teacher.setNom("Profil");
        teacher.setPrenom("Enseignant");
        teacher.setEmail(email);
        teacher.setActif(true);
        return teacherRepository.saveAndFlush(teacher);
    }

    private Responsable createUnlinkedGuardian(String email) {
        Responsable guardian = new Responsable();
        guardian.setNom("Profil");
        guardian.setPrenom("Responsable");
        guardian.setEmail(email);
        guardian.setActif(true);
        return guardianRepository.saveAndFlush(guardian);
    }

    private int provisionExistingTeacher(Long profileId, String email, String accessToken) throws Exception {
        return mockMvc.perform(post("/api/admin/accounts/teachers").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enseignantId", profileId, "email", email))))
                .andReturn().getResponse().getStatus();
    }

    private int provisionExistingGuardian(Long profileId, String email, String accessToken) throws Exception {
        return mockMvc.perform(post("/api/admin/accounts/guardians").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("responsableId", profileId, "email", email))))
                .andReturn().getResponse().getStatus();
    }

    private Utilisateur createActive(String email, Role role) {
        Utilisateur user = new Utilisateur();
        user.setEmailNormalise(email);
        user.setRole(role);
        user.setStatut(StatutUtilisateur.ACTIF);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setEmailVerifieAt(LocalDateTime.now());
        return userRepository.saveAndFlush(user);
    }

    private Utilisateur createPending(String email, Role role) {
        Utilisateur user = new Utilisateur();
        user.setEmailNormalise(email);
        user.setRole(role);
        user.setStatut(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
        return userRepository.saveAndFlush(user);
    }

    private String accessToken(Utilisateur user) { return accessTokenService.issue(user).value(); }

    private void failedLogin(String email) throws Exception {
        Csrf csrf = csrf();
        mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    private String forgot(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private Csrf csrf() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(result.getResponse().getCookie("XSRF-TOKEN"), body.get("headerName").asText(), body.get("token").asText());
    }

    private String rawToken(SimpleMailMessage message) {
        Matcher matcher = TOKEN.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private List<Boolean> concurrentAttempts(ThrowingRunnable action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> {
                ready.countDown(); start.await();
                try { action.run(); return true; } catch (RuntimeException ex) { return false; }
            };
            Future<Boolean> one = executor.submit(attempt); Future<Boolean> two = executor.submit(attempt);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private List<Boolean> concurrentAttempts(ThrowingRunnable firstAction, ThrowingRunnable secondAction) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = concurrentAttempt(firstAction, ready, start);
            Callable<Boolean> second = concurrentAttempt(secondAction, ready, start);
            Future<Boolean> one = executor.submit(first); Future<Boolean> two = executor.submit(second);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> concurrentAttempt(ThrowingRunnable action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown(); start.await();
            try { action.run(); return true; } catch (RuntimeException ex) { return false; }
        };
    }

    private List<Integer> concurrentHttpAttempts(Callable<Integer> firstAction, Callable<Integer> secondAction) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> first = () -> { ready.countDown(); start.await(); return firstAction.call(); };
            Callable<Integer> second = () -> { ready.countDown(); start.await(); return secondAction.call(); };
            Future<Integer> one = executor.submit(first); Future<Integer> two = executor.submit(second);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private interface ThrowingRunnable { void run(); }
    private record Csrf(Cookie cookie, String header, String token) { }
}
