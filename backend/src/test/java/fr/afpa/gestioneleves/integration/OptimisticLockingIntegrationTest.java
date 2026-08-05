package fr.afpa.gestioneleves.integration;

import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OptimisticLockingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("optimistic_locking").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void staleUtilisateurUpdateIsRejectedAndAllRequiredEntitiesExposeVersionMappings() throws Exception {
        assertThat(List.of(
                fr.afpa.gestioneleves.entity.Utilisateur.class,
                fr.afpa.gestioneleves.entity.Inscription.class,
                fr.afpa.gestioneleves.entity.Note.class,
                fr.afpa.gestioneleves.entity.Bulletin.class,
                fr.afpa.gestioneleves.entity.Enseignement.class))
                .allSatisfy(type -> assertThat(versionField(type).isAnnotationPresent(Version.class)).isTrue());

        long id = createPendingAdmin();
        EntityManager first = entityManagerFactory.createEntityManager();
        EntityManager second = entityManagerFactory.createEntityManager();
        try {
            first.getTransaction().begin();
            Utilisateur stale = first.find(Utilisateur.class, id);
            second.getTransaction().begin();
            Utilisateur current = second.find(Utilisateur.class, id);
            current.setTokenVersion(1);
            second.getTransaction().commit();

            stale.setTokenVersion(2);
            assertThatThrownBy(() -> first.getTransaction().commit())
                    .hasCauseInstanceOf(OptimisticLockException.class);
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            first.close(); second.close();
        }
    }

    private long createPendingAdmin() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Utilisateur user = new Utilisateur();
            user.setEmailNormalise("lock@example.fr");
            user.setRole(Role.ADMIN);
            user.setStatut(StatutUtilisateur.EN_ATTENTE_ACTIVATION);
            entityManager.persist(user);
            entityManager.getTransaction().commit();
            return user.getId();
        } finally {
            entityManager.close();
        }
    }

    private Field versionField(Class<?> type) {
        return List.of(type.getDeclaredFields()).stream()
                .filter(field -> field.isAnnotationPresent(Version.class))
                .findFirst().orElseThrow();
    }
}
