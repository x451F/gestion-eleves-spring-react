package fr.afpa.gestioneleves.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class PhaseOneMigrationTest {
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("phase_one_migration").withUsername("test").withPassword("test");

    @BeforeEach
    void cleanDatabase() {
        flyway(null).clean();
    }

    @Test
    void freshDatabaseMigratesAndEnforcesLifecycleTokenAndSessionFoundations() throws Exception {
        flyway(null).migrate();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, token_version, created_at, updated_at) "
                    + "VALUES ('admin@example.fr', NULL, 'ADMIN', 'EN_ATTENTE_ACTIVATION', 0, now(), now())");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, token_version, created_at, updated_at) "
                    + "VALUES ('invalid@example.fr', NULL, 'ADMIN', 'ACTIF', 0, now(), now())"))
                    .hasMessageContaining("ck_utilisateur_lifecycle_password");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, token_version, created_at, updated_at) "
                    + "VALUES ('ADMIN@example.fr', 'hash', 'ADMIN', 'ACTIF', 0, now(), now())"))
                    .hasMessageContaining("ck_utilisateur_email_normalise");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, email_verifie_at, token_version, created_at, updated_at) "
                    + "VALUES ('blank@example.fr', NULL, 'ADMIN', 'EN_ATTENTE_ACTIVATION', now(), 0, now(), now())"))
                    .hasMessageContaining("ck_utilisateur_lifecycle_password");
            execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, email_verifie_at, token_version, created_at, updated_at) "
                    + "VALUES ('active@example.fr', 'hash', 'ADMIN', 'ACTIF', now(), 0, now(), now()), ('disabled@example.fr', 'hash', 'ADMIN', 'DESACTIVE', now(), 0, now(), now())");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, token_version, created_at, updated_at) "
                    + "VALUES ('missing-verification@example.fr', 'hash', 'ADMIN', 'ACTIF', 0, now(), now())"))
                    .hasMessageContaining("ck_utilisateur_lifecycle_password");
            assertThatThrownBy(() -> execute(connection, "UPDATE utilisateur SET token_version = -1 WHERE email_normalise = 'active@example.fr'"))
                    .hasMessageContaining("ck_utilisateur_token_version");

            long userId = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = 'admin@example.fr'");
            execute(connection, "INSERT INTO activation_token (utilisateur_id, token_hash, expires_at, created_at) VALUES (" + userId + ", '" + HASH_A + "', now() + interval '1 day', now())");
            execute(connection, "INSERT INTO password_reset_token (utilisateur_id, token_hash, expires_at, created_at, revoked_at, revocation_reason) VALUES (" + userId + ", '" + HASH_B + "', now() + interval '30 minutes', now(), now(), 'superseded')");
            execute(connection, "INSERT INTO refresh_session_family (utilisateur_id, created_at, expires_at, device_label) VALUES (" + userId + ", now(), now() + interval '7 days', 'browser one')");
            execute(connection, "INSERT INTO refresh_session_family (utilisateur_id, created_at, expires_at, device_label) VALUES (" + userId + ", now(), now() + interval '7 days', 'browser two')");
            long familyId = id(connection, "SELECT min(id) FROM refresh_session_family");
            execute(connection, "INSERT INTO refresh_session (family_id, generation, token_hash, created_at, expires_at) VALUES (" + familyId + ", 0, '" + HASH_C + "', now(), now() + interval '7 days')");

            assertThat(count(connection, "SELECT count(*) FROM refresh_session_family WHERE utilisateur_id = " + userId)).isEqualTo(2);
            assertThat(count(connection, "SELECT count(*) FROM information_schema.columns WHERE table_name IN ('activation_token', 'password_reset_token', 'refresh_session') AND column_name ILIKE '%raw%'")).isZero();
            assertThat(count(connection, "SELECT count(*) FROM information_schema.columns WHERE table_name = 'refresh_session' AND column_name = 'replaced_by_id'")).isEqualTo(1);
        }
    }

    @Test
    void representativeV1DataUpgradesWithoutLosingRelationships() throws Exception {
        flyway("1").migrate();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO utilisateur (id, email_normalise, password_hash, role, actif, created_at, updated_at) VALUES (10, ' Alice@Example.FR ', 'legacy-hash', 'ENSEIGNANT', true, now(), now())");
            execute(connection, "INSERT INTO enseignant (id, matricule, nom, prenom, email, utilisateur_id, actif, created_at, updated_at) VALUES (20, 'ENS-20', 'Durand', 'Alice', 'alice@example.fr', 10, true, now(), now())");
            execute(connection, "INSERT INTO eleve (id, numero_dossier, nom, prenom, date_naissance, actif, created_at, updated_at) VALUES (30, 'ELEVE-30', 'Martin', 'Lina', '2014-01-02', true, now(), now())");
            execute(connection, "INSERT INTO classe (id, code, nom, niveau, annee_scolaire, actif, created_at, updated_at) VALUES (40, 'C40', 'CM2 A', 'CM2', '2026-2027', true, now(), now())");
            execute(connection, "INSERT INTO inscription (id, eleve_id, classe_id, annee_scolaire, date_inscription, statut, created_at, updated_at) VALUES (50, 30, 40, '2026-2027', '2026-09-01', 'ACTIVE', now(), now())");
            execute(connection, "INSERT INTO responsable (id, nom, prenom, email, actif, created_at, updated_at) VALUES (60, 'Martin', 'Sam', 'sam@example.fr', true, now(), now())");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, created_at, updated_at) VALUES (30, 60, 'PERE', true, now(), now())");
        }
        flyway(null).migrate();
        try (Connection connection = connection()) {
            assertThat(value(connection, "SELECT email_normalise FROM utilisateur WHERE id = 10")).isEqualTo("alice@example.fr");
            assertThat(value(connection, "SELECT statut FROM utilisateur WHERE id = 10")).isEqualTo("ACTIF");
            assertThat(value(connection, "SELECT email_verifie_at::text FROM utilisateur WHERE id = 10")).isNotBlank();
            assertThat(value(connection, "SELECT legacy_email FROM enseignant WHERE id = 20")).isEqualTo("alice@example.fr");
            assertThat(value(connection, "SELECT statut FROM inscription WHERE id = 50")).isEqualTo("EN_COURS");
            assertThat(value(connection, "SELECT valid_from::text FROM eleve_responsable WHERE eleve_id = 30 AND responsable_id = 60")).isNotBlank();
        }
    }

    @Test
    void conflictingLinkedProfileEmailStopsUpgradeInsteadOfOverwritingIt() throws Exception {
        flyway("1").migrate();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO utilisateur (id, email_normalise, password_hash, role, actif, created_at, updated_at) VALUES (1, 'teacher@example.fr', 'hash', 'ENSEIGNANT', true, now(), now())");
            execute(connection, "INSERT INTO enseignant (matricule, nom, prenom, email, utilisateur_id, actif, created_at, updated_at) VALUES ('ENS-1', 'A', 'B', 'other@example.fr', 1, true, now(), now())");
        }
        assertThatThrownBy(() -> flyway(null).migrate())
                .hasMessageContaining("linked profile role or email conflicts");
    }

    @Test
    void disabledPasswordlessLegacyAccountStopsUpgradeWithoutChangingIntent() throws Exception {
        flyway("1").migrate();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO utilisateur (id, email_normalise, role, actif, created_at, updated_at) VALUES (42, 'disabled@example.fr', 'ADMIN', false, now(), now())");
        }
        assertThatThrownBy(() -> flyway(null).migrate())
                .hasMessageContaining("disabled passwordless account has ambiguous lifecycle")
                .hasMessageContaining("42");
    }

    @Test
    void blankLegacyCanonicalEmailStopsUpgradeAndBlankFreshEmailIsRejected() throws Exception {
        flyway("1").migrate();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO utilisateur (id, email_normalise, password_hash, role, actif, created_at, updated_at) VALUES (43, '   ', 'hash', 'ADMIN', true, now(), now())");
        }
        assertThatThrownBy(() -> flyway(null).migrate())
                .hasMessageContaining("blank canonical email")
                .hasMessageContaining("43");

        flyway(null).clean();
        flyway(null).migrate();
        try (Connection connection = connection()) {
            assertThatThrownBy(() -> execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, email_verifie_at, token_version, created_at, updated_at) VALUES ('   ', 'hash', 'ADMIN', 'ACTIF', now(), 0, now(), now())"))
                    .hasMessageContaining("ck_utilisateur_email_normalise");
        }
    }

    @Test
    void profileAndHistoricalConstraintsRejectInvalidCurrentState() throws Exception {
        flyway(null).migrate();
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, token_version, created_at, updated_at) VALUES ('teacher@example.fr', NULL, 'ENSEIGNANT', 'EN_ATTENTE_ACTIVATION', 0, now(), now())");
            long teacherUserId = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = 'teacher@example.fr'");
            execute(connection, "INSERT INTO enseignant (matricule, nom, prenom, email, utilisateur_id, actif, created_at, updated_at) VALUES ('ENS-1', 'Teacher', 'One', 'teacher@example.fr', " + teacherUserId + ", true, now(), now())");
            connection.commit();
            connection.setAutoCommit(true);
            assertThatThrownBy(() -> execute(connection, "UPDATE utilisateur SET role = 'ADMIN' WHERE id = " + teacherUserId))
                    .hasMessageContaining("role is immutable");

            execute(connection, "INSERT INTO eleve (id, numero_dossier, nom, prenom, date_naissance, actif, created_at, updated_at) VALUES (1, 'E1', 'Pupil', 'One', '2015-01-01', true, now(), now())");
            execute(connection, "INSERT INTO classe (id, code, nom, niveau, annee_scolaire, actif, created_at, updated_at) VALUES (1, 'C1', 'Class 1', 'CM1', '2026-2027', true, now(), now()), (2, 'C2', 'Class 2', 'CM1', '2027-2028', true, now(), now())");
            execute(connection, "INSERT INTO inscription (eleve_id, classe_id, annee_scolaire, date_inscription, statut, created_at, updated_at) VALUES (1, 1, '2026-2027', '2026-09-01', 'EN_COURS', now(), now())");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO inscription (eleve_id, classe_id, annee_scolaire, date_inscription, statut, created_at, updated_at) VALUES (1, 2, '2027-2028', '2027-09-01', 'EN_COURS', now(), now())"))
                    .hasMessageContaining("uk_inscription_en_cours_eleve");

            execute(connection, "INSERT INTO responsable (id, nom, prenom, email, actif, created_at, updated_at) VALUES (1, 'Guardian', 'One', 'guardian1@example.fr', true, now(), now()), (2, 'Guardian', 'Two', 'guardian2@example.fr', true, now(), now())");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, created_at, updated_at) VALUES (1, 1, 'PERE', true, '2026-09-01', now(), now())");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, created_at, updated_at) VALUES (1, 2, 'MERE', true, '2026-09-01', now(), now())"))
                    .hasMessageContaining("ex_eleve_responsable_principal_validity");
            assertThatThrownBy(() -> execute(connection, "DELETE FROM responsable WHERE id = 1"))
                    .hasMessageContaining("fk_eleve_responsable_responsable");
        }
    }

    @Test
    void deferredProfileReassignmentValidatesBothAccountsAndRoleNeverChanges() throws Exception {
        flyway(null).migrate();
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            insertPendingTeacher(connection, "teacher-one@example.fr", "ENS-ONE");
            insertPendingTeacher(connection, "teacher-two@example.fr", "ENS-TWO");
            long firstUser = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = 'teacher-one@example.fr'");
            long secondUser = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = 'teacher-two@example.fr'");
            long firstProfile = id(connection, "SELECT id FROM enseignant WHERE matricule = 'ENS-ONE'");
            long secondProfile = id(connection, "SELECT id FROM enseignant WHERE matricule = 'ENS-TWO'");
            connection.commit();

            connection.setAutoCommit(false);
            execute(connection, "UPDATE enseignant SET utilisateur_id = NULL WHERE id = " + firstProfile);
            execute(connection, "UPDATE enseignant SET utilisateur_id = " + firstUser + " WHERE id = " + secondProfile);
            execute(connection, "UPDATE enseignant SET utilisateur_id = " + secondUser + " WHERE id = " + firstProfile);
            connection.commit();

            connection.setAutoCommit(false);
            execute(connection, "UPDATE enseignant SET utilisateur_id = NULL WHERE id = " + firstProfile);
            assertThatThrownBy(connection::commit)
                    .hasMessageContaining("Account role/profile invariant violated");
            connection.rollback();

            connection.setAutoCommit(false);
            execute(connection, "UPDATE enseignant SET utilisateur_id = NULL WHERE id = " + firstProfile);
            execute(connection, "INSERT INTO responsable (nom, prenom, email, utilisateur_id, actif, created_at, updated_at) VALUES ('Replacement', 'Guardian', 'replacement@example.fr', " + secondUser + ", true, now(), now())");
            assertThatThrownBy(() -> execute(connection, "UPDATE utilisateur SET role = 'RESPONSABLE' WHERE id = " + secondUser))
                    .hasMessageContaining("role is immutable");
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    @Test
    void refreshLineageUsesImmutableSameFamilyForwardGenerations() throws Exception {
        flyway(null).migrate();
        try (Connection connection = connection()) {
            insertActiveAdmin(connection, "lineage-one@example.fr");
            insertActiveAdmin(connection, "lineage-two@example.fr");
            long firstUser = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = 'lineage-one@example.fr'");
            long secondUser = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = 'lineage-two@example.fr'");
            execute(connection, "INSERT INTO refresh_session_family (id, utilisateur_id, created_at, expires_at) VALUES (101, " + firstUser + ", now(), now() + interval '7 days'), (102, " + firstUser + ", now(), now() + interval '7 days'), (103, " + secondUser + ", now(), now() + interval '7 days')");
            execute(connection, "INSERT INTO refresh_session (id, family_id, generation, token_hash, created_at, expires_at) VALUES (101, 101, 0, '" + "d".repeat(64) + "', now(), now() + interval '7 days'), (102, 101, 1, '" + "e".repeat(64) + "', now(), now() + interval '7 days'), (105, 101, 2, '" + "a".repeat(64) + "', now(), now() + interval '7 days'), (106, 101, 3, '" + "b".repeat(64) + "', now(), now() + interval '7 days'), (103, 102, 1, '" + "f".repeat(64) + "', now(), now() + interval '7 days'), (104, 103, 1, '" + "1".repeat(64) + "', now(), now() + interval '7 days')");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_id = 103, replaced_by_generation = 1 WHERE id = 101"))
                    .hasMessageContaining("fk_refresh_session_replacement_same_family");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_id = 104, replaced_by_generation = 1 WHERE id = 101"))
                    .hasMessageContaining("fk_refresh_session_replacement_same_family");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_id = 101, replaced_by_generation = 0 WHERE id = 101"))
                    .hasMessageContaining("violates check constraint");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_id = 101, replaced_by_generation = 0 WHERE id = 102"))
                    .hasMessageContaining("violates check constraint");
            execute(connection, "UPDATE refresh_session SET replaced_by_id = 102, replaced_by_generation = 1 WHERE id = 101");
            execute(connection, "UPDATE refresh_session SET replaced_by_id = 105, replaced_by_generation = 2 WHERE id = 102");
            execute(connection, "UPDATE refresh_session SET replaced_by_id = 102, replaced_by_generation = 1 WHERE id = 101");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_id = NULL, replaced_by_generation = NULL WHERE id = 101"))
                    .hasMessageContaining("replacement can only be assigned once");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_generation = 2 WHERE id = 101"))
                    .hasMessageContaining("replacement can only be assigned once");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET replaced_by_id = 106, replaced_by_generation = 3 WHERE id = 101"))
                    .hasMessageContaining("replacement can only be assigned once");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET family_id = 102 WHERE id = 102"))
                    .hasMessageContaining("family membership is immutable");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET family_id = 102 WHERE id = 101"))
                    .hasMessageContaining("family membership is immutable");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session SET generation = 2 WHERE id = 102"))
                    .hasMessageContaining("generation is immutable");
            assertThatThrownBy(() -> execute(connection, "UPDATE refresh_session_family SET utilisateur_id = " + secondUser + " WHERE id = 101"))
                    .hasMessageContaining("owner cannot change after sessions exist");
        }
    }

    @Test
    void concurrentOpposingReplacementUpdatesCannotBothCommit() throws Exception {
        flyway(null).migrate();
        try (Connection setup = connection(); Connection first = connection(); Connection second = connection()) {
            insertActiveAdmin(setup, "concurrent@example.fr");
            long userId = id(setup, "SELECT id FROM utilisateur WHERE email_normalise = 'concurrent@example.fr'");
            execute(setup, "INSERT INTO refresh_session_family (id, utilisateur_id, created_at, expires_at) VALUES (201, " + userId + ", now(), now() + interval '7 days')");
            execute(setup, "INSERT INTO refresh_session (id, family_id, generation, token_hash, created_at, expires_at) VALUES (201, 201, 0, '" + "2".repeat(64) + "', now(), now() + interval '7 days'), (202, 201, 1, '" + "3".repeat(64) + "', now(), now() + interval '7 days')");

            first.setAutoCommit(false);
            second.setAutoCommit(false);
            execute(first, "UPDATE refresh_session SET replaced_by_id = 202, replaced_by_generation = 1 WHERE id = 201");
            execute(second, "SET LOCAL lock_timeout = '250ms'");
            assertThatThrownBy(() -> execute(second, "UPDATE refresh_session SET replaced_by_id = 201, replaced_by_generation = 0 WHERE id = 202"))
                    .hasMessageMatching("(?s).*(violates check constraint|canceling statement due to lock timeout).*");
            second.rollback();
            first.commit();
            assertThatThrownBy(() -> execute(setup, "UPDATE refresh_session SET replaced_by_id = 201, replaced_by_generation = 0 WHERE id = 202"))
                    .hasMessageContaining("violates check constraint");
            assertThat(value(setup, "SELECT replaced_by_id::text FROM refresh_session WHERE id = 201")).isEqualTo("202");
        }
    }

    @Test
    void principalValidityUsesIntervalOverlapInsteadOfOnlyOpenEndedRows() throws Exception {
        flyway(null).migrate();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO eleve (id, numero_dossier, nom, prenom, date_naissance, actif, created_at, updated_at) VALUES (101, 'E101', 'Pupil', 'Intervals', '2015-01-01', true, now(), now())");
            execute(connection, "INSERT INTO responsable (id, nom, prenom, email, actif, created_at, updated_at) VALUES (101, 'A', 'One', 'a101@example.fr', true, now(), now()), (102, 'B', 'Two', 'b102@example.fr', true, now(), now()), (103, 'C', 'Three', 'c103@example.fr', true, now(), now()), (104, 'D', 'Four', 'd104@example.fr', true, now(), now()), (105, 'E', 'Five', 'e105@example.fr', true, now(), now()), (106, 'F', 'Six', 'f106@example.fr', true, now(), now())");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, valid_to, created_at, updated_at) VALUES (101, 101, 'PERE', true, '2026-01-01', '2026-06-30', now(), now())");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, valid_to, created_at, updated_at) VALUES (101, 102, 'MERE', true, '2026-06-01', '2026-12-31', now(), now())"))
                    .hasMessageContaining("ex_eleve_responsable_principal_validity");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, valid_to, created_at, updated_at) VALUES (101, 102, 'MERE', true, '2026-07-01', '2026-12-31', now(), now())");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, created_at, updated_at) VALUES (101, 103, 'TUTEUR', true, '2027-01-01', now(), now())");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, created_at, updated_at) VALUES (101, 104, 'TUTEUR', true, '2027-06-01', now(), now())"))
                    .hasMessageContaining("ex_eleve_responsable_principal_validity");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, created_at, updated_at) VALUES (101, 105, 'AUTRE', false, '2026-01-01', now(), now())");
            execute(connection, "INSERT INTO eleve_responsable (eleve_id, responsable_id, lien_parente, responsable_principal, valid_from, created_at, updated_at) VALUES (101, 106, 'AUTRE', false, '2026-01-01', now(), now())");
            assertThatThrownBy(() -> execute(connection, "UPDATE eleve_responsable SET valid_to = valid_from - 1 WHERE eleve_id = 101 AND responsable_id = 101"))
                    .hasMessageContaining("ck_eleve_responsable_validity");
        }
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false);
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.execute(); }
    }

    private void insertPendingTeacher(Connection connection, String email, String matricule) throws Exception {
        execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, token_version, created_at, updated_at) VALUES ('" + email + "', NULL, 'ENSEIGNANT', 'EN_ATTENTE_ACTIVATION', 0, now(), now())");
        long userId = id(connection, "SELECT id FROM utilisateur WHERE email_normalise = '" + email + "'");
        execute(connection, "INSERT INTO enseignant (matricule, nom, prenom, email, utilisateur_id, actif, created_at, updated_at) VALUES ('" + matricule + "', 'Teacher', 'Test', '" + email + "', " + userId + ", true, now(), now())");
    }

    private void insertActiveAdmin(Connection connection, String email) throws Exception {
        execute(connection, "INSERT INTO utilisateur (email_normalise, password_hash, role, statut, email_verifie_at, token_version, created_at, updated_at) VALUES ('" + email + "', 'hash', 'ADMIN', 'ACTIF', now(), 0, now(), now())");
    }

    private long id(Connection connection, String sql) throws Exception { return Long.parseLong(value(connection, sql)); }
    private long count(Connection connection, String sql) throws Exception { return Long.parseLong(value(connection, sql)); }
    private String value(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            resultSet.next(); return resultSet.getString(1);
        }
    }
}
