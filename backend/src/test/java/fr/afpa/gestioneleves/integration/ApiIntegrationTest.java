package fr.afpa.gestioneleves.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@WithMockUser(roles = "ADMIN")
class ApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("gestion_eleves_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configurerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AccessPolicyService accessPolicyService;

    @Test
    void crudEleveComplet() throws Exception {
        long eleveId = creerEleve("DOS-CRUD", "Lina", "Martin");

        mockMvc.perform(get("/api/eleves/{id}", eleveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDossier").value("DOS-CRUD"))
                .andExpect(jsonPath("$.prenom").value("Lina"));

        mockMvc.perform(put("/api/eleves/{id}", eleveId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroDossier": "DOS-CRUD",
                                  "nom": "Martin",
                                  "prenom": "Lina",
                                  "dateNaissance": "2012-04-02",
                                  "email": "lina.martin@example.fr",
                                  "telephone": "0611223344"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lina.martin@example.fr"));

        mockMvc.perform(get("/api/eleves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eleveId));

        mockMvc.perform(delete("/api/eleves/{id}", eleveId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/eleves/{id}", eleveId))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationEtUniciteEleveSontExposeesParLApi() throws Exception {
        creerEleve("DOS-UNIQUE", "Noé", "Petit");

        mockMvc.perform(post("/api/eleves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eleveJson("DOS-UNIQUE", "Autre", "Élève")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/eleves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroDossier": "",
                                  "nom": "",
                                  "prenom": "",
                                  "dateNaissance": "2035-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inscriptionId": 999,
                                  "enseignementId": 999,
                                  "valeur": 21.0,
                                  "bareme": 20.0,
                                  "coefficient": 1.0,
                                  "dateEvaluation": "2026-07-29",
                                  "periode": "TRIMESTRE_1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'valeur')]").isNotEmpty());
    }

    @Test
    void parcoursMetierCompletProduitBulletinPdfEtPhoto() throws Exception {
        long eleveId = creerEleve("DOS-PARCOURS", "Zoé", "Bernard");
        long classeId = creer("/api/classes", """
                {
                  "code": "6A-2026",
                  "nom": "6e A",
                  "niveau": "6e",
                  "anneeScolaire": "2026-2027"
                }
                """);
        long inscriptionId = creer("/api/inscriptions", """
                {
                  "eleveId": %d,
                  "classeId": %d,
                  "anneeScolaire": "2026-2027",
                  "dateInscription": "2026-07-29",
                  "statut": "ACTIVE"
                }
                """.formatted(eleveId, classeId));
        long enseignantId = creer("/api/enseignants", """
                {
                  "matricule": "ENS-001",
                  "nom": "Durand",
                  "prenom": "Alice",
                  "email": "alice.durand@example.fr"
                }
                """);
        long matiereId = creer("/api/matieres", """
                {
                  "code": "MATH",
                  "nom": "Mathématiques",
                  "coefficientDefaut": 2.0
                }
                """);
        long enseignementId = creer("/api/enseignements", """
                {
                  "enseignantId": %d,
                  "matiereId": %d,
                  "classeId": %d,
                  "anneeScolaire": "2026-2027",
                  "coefficientMatiere": 2.0
                }
                """.formatted(enseignantId, matiereId, classeId));

        long noteId = creer("/api/notes", """
                {
                  "inscriptionId": %d,
                  "enseignementId": %d,
                  "valeur": 15.0,
                  "bareme": 20.0,
                  "coefficient": 1.0,
                  "dateEvaluation": "2026-07-29",
                  "periode": "TRIMESTRE_1",
                  "commentaire": "Travail sérieux"
                }
                """.formatted(inscriptionId, enseignementId));

        mockMvc.perform(get("/api/inscriptions/{id}/notes", inscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(noteId))
                .andExpect(jsonPath("$[0].valeur").value(15.0));

        long bulletinId = creer("/api/bulletins/generate", """
                {
                  "inscriptionId": %d,
                  "periode": "TRIMESTRE_1",
                  "appreciation": "Très bon trimestre"
                }
                """.formatted(inscriptionId));

        mockMvc.perform(get("/api/bulletins/{id}", bulletinId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moyenneGenerale").value(15.0))
                .andExpect(jsonPath("$.lignes[0].nomMatiere").value("Mathématiques"));

        byte[] pdf = mockMvc.perform(get("/api/bulletins/{id}/pdf", bulletinId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).startsWith("%PDF".getBytes());

        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
        MockMultipartFile photo = new MockMultipartFile("file", "portrait.png", "image/png", png);
        mockMvc.perform(multipart("/api/eleves/{id}/photo", eleveId).file(photo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeMime").value("image/png"));

        mockMvc.perform(get("/api/eleves/{id}/photo", eleveId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));

        mockMvc.perform(delete("/api/eleves/{id}/photo", eleveId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/inscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleveId": %d,
                                  "classeId": %d,
                                  "anneeScolaire": "2026-2027",
                                  "dateInscription": "2026-07-29",
                                  "statut": "ACTIVE"
                                }
                                """.formatted(eleveId, classeId)))
                .andExpect(status().isConflict());
    }

    @Test
    void coherenceDesNotesEtValidationDesPhotosSontControlees() throws Exception {
        long eleveId = creerEleve("DOS-CONTROLE", "Malo", "Robert");
        long classeEleveId = creer("/api/classes", """
                {"code":"5A-2026","nom":"5e A","niveau":"5e","anneeScolaire":"2026-2027"}
                """);
        long autreClasseId = creer("/api/classes", """
                {"code":"5B-2026","nom":"5e B","niveau":"5e","anneeScolaire":"2026-2027"}
                """);
        long inscriptionId = creer("/api/inscriptions", """
                {
                  "eleveId": %d,
                  "classeId": %d,
                  "anneeScolaire": "2026-2027",
                  "dateInscription": "2026-07-29",
                  "statut": "ACTIVE"
                }
                """.formatted(eleveId, classeEleveId));
        long enseignantId = creer("/api/enseignants", """
                {"matricule":"ENS-002","nom":"Moreau","prenom":"Paul","email":"paul.moreau@example.fr"}
                """);
        long matiereId = creer("/api/matieres", """
                {"code":"HIST","nom":"Histoire","coefficientDefaut":1.0}
                """);
        long enseignementId = creer("/api/enseignements", """
                {
                  "enseignantId": %d,
                  "matiereId": %d,
                  "classeId": %d,
                  "anneeScolaire": "2026-2027",
                  "coefficientMatiere": 1.0
                }
                """.formatted(enseignantId, matiereId, autreClasseId));

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inscriptionId": %d,
                                  "enseignementId": %d,
                                  "valeur": 12.0,
                                  "bareme": 20.0,
                                  "coefficient": 1.0,
                                  "dateEvaluation": "2026-07-29",
                                  "periode": "TRIMESTRE_1"
                                }
                                """.formatted(inscriptionId, enseignementId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        MockMultipartFile fauxPng =
                new MockMultipartFile("file", "faux.png", "image/png", "pas une image".getBytes());
        mockMvc.perform(multipart("/api/eleves/{id}/photo", eleveId).file(fauxPng))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    private long creerEleve(String dossier, String prenom, String nom) throws Exception {
        return creer("/api/eleves", eleveJson(dossier, prenom, nom));
    }

    private String eleveJson(String dossier, String prenom, String nom) {
        return """
                {
                  "numeroDossier": "%s",
                  "nom": "%s",
                  "prenom": "%s",
                  "dateNaissance": "2012-04-02"
                }
                """.formatted(dossier, nom, prenom);
    }

    private long creer(String chemin, String json) throws Exception {
        String corps = mockMvc.perform(post(chemin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode resultat = objectMapper.readTree(corps);
        return resultat.path("id").asLong();
    }
}
