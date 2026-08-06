package fr.afpa.gestioneleves.integration;

import fr.afpa.gestioneleves.entity.*;
import fr.afpa.gestioneleves.enumtype.*;
import fr.afpa.gestioneleves.repository.*;
import fr.afpa.gestioneleves.security.RefreshTokenService;
import fr.afpa.gestioneleves.service.RefreshSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Testcontainers
@Transactional
class AuthorizationIntegrationTest {
 @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("authorization").withUsername("test").withPassword("test");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r) { r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername); r.add("spring.datasource.password", POSTGRES::getPassword); }
 @Autowired MockMvc mvc; @Autowired JwtEncoder encoder; @Autowired UtilisateurRepository users; @Autowired EnseignantRepository teachers; @Autowired ResponsableRepository guardians; @Autowired EleveRepository pupils; @Autowired ClasseRepository classes; @Autowired MatiereRepository matieres; @Autowired EnseignementRepository teachings; @Autowired InscriptionRepository registrations; @Autowired NoteRepository notes; @Autowired BulletinRepository bulletins; @Autowired EleveResponsableRepository links; @Autowired RefreshSessionService sessions; @Autowired RefreshSessionFamilyRepository families;

 @Test void authenticationBoundaryAdminDeactivationAndLastAdminProtection() throws Exception {
  Utilisateur admin = user("admin", Role.ADMIN); Utilisateur teacher = user("teacher", Role.ENSEIGNANT); teacherProfile(teacher, "T1");
  mvc.perform(get("/api/eleves")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
  sessions.createFamily(teacher);
  mvc.perform(post("/api/admin/accounts/{id}/deactivate", teacher.getId()).header("Authorization", bearer(admin))).andExpect(status().isNoContent());
  assertThat(users.findById(teacher.getId()).orElseThrow().getStatut()).isEqualTo(StatutUtilisateur.DESACTIVE);
  assertThat(users.findById(teacher.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
  assertThat(families.findByUtilisateurId(teacher.getId())).allMatch(f -> f.getRevokedAt() != null);
  mvc.perform(get("/api/enseignants/{id}", teachers.findByUtilisateurId(teacher.getId()).orElseThrow().getId()).header("Authorization", bearer(teacher))).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/accounts/{id}/deactivate", admin.getId()).header("Authorization", bearer(admin))).andExpect(status().isBadRequest());
 }
 @Test void teacherOwnershipAndScopedListHideForeignResources() throws Exception {
  Utilisateur admin=user("admin",Role.ADMIN), a=user("ta",Role.ENSEIGNANT), b=user("tb",Role.ENSEIGNANT); Enseignant ta=teacherProfile(a,"TA"), tb=teacherProfile(b,"TB");
  Eleve ea=pupil("EA"), eb=pupil("EB"); Classe ca=classe("CA"), cb=classe("CB"), historic=classe("CH","2024-2025"); Inscription ia=registration(ea,ca), ib=registration(eb,cb), old=registration(ea,historic,StatutInscription.TERMINEE); Enseignement eta=teaching(ta,ca), otherSubjectInSameClass=teaching(tb,ca), etb=teaching(tb,cb), oldTeaching=teaching(tb,historic); Note own=note(ia,eta), foreignSameStudent=note(ia,otherSubjectInSameClass), foreign=note(ib,etb), oldNote=note(old,oldTeaching); Bulletin oldBulletin=bulletin(old,StatutBulletin.PUBLIE);
  mvc.perform(get("/api/eleves").header("Authorization",bearer(a))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(ea.getId()));
  mvc.perform(get("/api/eleves/{id}",ea.getId()).header("Authorization",bearer(a))).andExpect(status().isOk());
  mvc.perform(get("/api/eleves/{id}",eb.getId()).header("Authorization",bearer(a))).andExpect(status().isNotFound());
  mvc.perform(get("/api/notes/{id}",foreign.getId()).header("Authorization",bearer(a))).andExpect(status().isNotFound());
  mvc.perform(get("/api/notes/{id}",foreignSameStudent.getId()).header("Authorization",bearer(a))).andExpect(status().isNotFound());
  mvc.perform(get("/api/inscriptions/{id}",old.getId()).header("Authorization",bearer(a))).andExpect(status().isNotFound());
  mvc.perform(get("/api/notes/{id}",oldNote.getId()).header("Authorization",bearer(a))).andExpect(status().isNotFound());
  mvc.perform(get("/api/bulletins/{id}/pdf",oldBulletin.getId()).header("Authorization",bearer(a))).andExpect(status().isNotFound());
  mvc.perform(get("/api/eleves/{id}/inscriptions",ea.getId()).header("Authorization",bearer(a))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(ia.getId())).andExpect(jsonPath("$[1]").doesNotExist());
  mvc.perform(get("/api/eleves/{id}/notes",ea.getId()).header("Authorization",bearer(a))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(own.getId())).andExpect(jsonPath("$[1]").doesNotExist());
  mvc.perform(get("/api/inscriptions/{id}/notes",ia.getId()).header("Authorization",bearer(a))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(own.getId())).andExpect(jsonPath("$[1]").doesNotExist());
  mvc.perform(post("/api/classes").header("Authorization",bearer(a)).contentType("application/json").content("{\"code\":\"X\",\"nom\":\"X\",\"niveau\":\"6e\",\"anneeScolaire\":\"2025-2026\"}")).andExpect(status().isForbidden());
 }
 @Test void currentTeacherEndpointsAndClassAssignmentsAreScopedAndSupportFirstNote() throws Exception {
  Utilisateur admin=user("admin-me",Role.ADMIN), a=user("teacher-a",Role.ENSEIGNANT), b=user("teacher-b",Role.ENSEIGNANT), empty=user("teacher-empty",Role.ENSEIGNANT), guardian=user("guardian-me",Role.RESPONSABLE);
  Enseignant ta=teacherProfile(a,"TA"), tb=teacherProfile(b,"TB"); teacherProfile(empty,"TE");
  Classe classe=classe("ME"); Eleve eleve=pupil("FIRST"); Inscription inscription=registration(eleve,classe);
  Enseignement own=teaching(ta,classe), foreign=teaching(tb,classe);

  mvc.perform(get("/api/enseignants/me")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/enseignants/me").header("Authorization",bearer(a)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ta.getId())).andExpect(jsonPath("$.utilisateurId").value(a.getId()));
  mvc.perform(get("/api/enseignants/me/enseignements").header("Authorization",bearer(a)))
          .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(own.getId())).andExpect(jsonPath("$[0].enseignantId").value(ta.getId())).andExpect(jsonPath("$[1]").doesNotExist());
  mvc.perform(get("/api/enseignants/me/enseignements").header("Authorization",bearer(empty)))
          .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
  mvc.perform(get("/api/enseignants/me").header("Authorization",bearer(guardian))).andExpect(status().isForbidden());
  mvc.perform(get("/api/classes/{id}/enseignements",classe.getId()).header("Authorization",bearer(a)))
          .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(own.getId())).andExpect(jsonPath("$[0].matiereNom").value(own.getMatiere().getNom())).andExpect(jsonPath("$[1]").doesNotExist());
  mvc.perform(get("/api/classes/{id}/enseignements",classe.getId()).header("Authorization",bearer(admin)))
          .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists()).andExpect(jsonPath("$[1].id").exists());
  mvc.perform(get("/api/classes/{id}/enseignements",classe.getId()).header("Authorization",bearer(guardian))).andExpect(status().isForbidden());

  String ownPayload="{\"inscriptionId\":%d,\"enseignementId\":%d,\"periode\":\"TRIMESTRE_1\",\"valeur\":12,\"bareme\":20,\"coefficient\":1,\"dateEvaluation\":\"%s\",\"libelle\":\"Première évaluation\",\"commentaire\":null}".formatted(inscription.getId(),own.getId(),LocalDate.now());
  mvc.perform(post("/api/notes").header("Authorization",bearer(a)).contentType("application/json").content(ownPayload))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.enseignementId").value(own.getId()));
  String foreignPayload="{\"inscriptionId\":%d,\"enseignementId\":%d,\"periode\":\"TRIMESTRE_1\",\"valeur\":12,\"bareme\":20,\"coefficient\":1,\"dateEvaluation\":\"%s\",\"libelle\":\"Interdit\",\"commentaire\":null}".formatted(inscription.getId(),foreign.getId(),LocalDate.now());
  mvc.perform(post("/api/notes").header("Authorization",bearer(a)).contentType("application/json").content(foreignPayload)).andExpect(status().isNotFound());
 }
 @Test void guardianOwnershipHidesForeignStudentsNotesAndDraftsButAllowsPublishedBulletinPdf() throws Exception {
  Utilisateur g1=user("g1",Role.RESPONSABLE), g2=user("g2",Role.RESPONSABLE); Responsable r1=guardianProfile(g1), r2=guardianProfile(g2); Eleve e1=pupil("G1"), e2=pupil("G2"), expired=pupil("GX"); Classe c=classe("CG"); Inscription i1=registration(e1,c), i2=registration(e2,c), ix=registration(expired,c,StatutInscription.TERMINEE); link(r1,e1); link(r2,e2); EleveResponsable ended=new EleveResponsable();ended.setResponsable(r1);ended.setEleve(expired);ended.setLienParente(LienParente.PERE);ended.setValidFrom(LocalDate.now().minusYears(2));ended.setValidTo(LocalDate.now().minusDays(1));links.saveAndFlush(ended); Bulletin draft=bulletin(i1,StatutBulletin.BROUILLON), published=bulletin(i1,StatutBulletin.PUBLIE, PeriodeBulletin.TRIMESTRE_2), expiredBulletin=bulletin(ix,StatutBulletin.PUBLIE);
  mvc.perform(get("/api/eleves").header("Authorization",bearer(g1))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(e1.getId()));
  mvc.perform(get("/api/responsables/{id}/eleves",r1.getId()).header("Authorization",bearer(g1))).andExpect(status().isOk()).andExpect(jsonPath("$[0].eleveId").value(e1.getId())).andExpect(jsonPath("$[1]").doesNotExist());
  mvc.perform(get("/api/eleves/{id}",e2.getId()).header("Authorization",bearer(g1))).andExpect(status().isNotFound());
  mvc.perform(get("/api/eleves/{id}",expired.getId()).header("Authorization",bearer(g1))).andExpect(status().isNotFound());
  mvc.perform(get("/api/bulletins/{id}/pdf",expiredBulletin.getId()).header("Authorization",bearer(g1))).andExpect(status().isNotFound());
  mvc.perform(get("/api/bulletins/{id}",draft.getId()).header("Authorization",bearer(g1))).andExpect(status().isNotFound());
  mvc.perform(get("/api/bulletins/{id}/pdf",published.getId()).header("Authorization",bearer(g1))).andExpect(status().isOk());
  mvc.perform(post("/api/notes").header("Authorization",bearer(g1)).contentType("application/json").content("{\"inscriptionId\":1,\"enseignementId\":1,\"periode\":\"TRIMESTRE_1\",\"valeur\":10,\"bareme\":20,\"coefficient\":1,\"dateEvaluation\":\"2025-01-01\",\"libelle\":\"x\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/notes").header("Authorization",bearer(g1))).andExpect(status().isForbidden());
 }
 private Utilisateur user(String s,Role role){Utilisateur u=new Utilisateur();u.setEmailNormalise(s+"@x.fr");u.setRole(role);u.setStatut(StatutUtilisateur.ACTIF);u.setPasswordHash("{bcrypt}$2a$12$abcdefghijklmnopqrstuuQfX2Yl0c2GqvJz0xV0M0b3lqvSkPp9zEu");u.setEmailVerifieAt(LocalDateTime.now());return users.saveAndFlush(u);}
 private Enseignant teacherProfile(Utilisateur u,String m){Enseignant e=new Enseignant();e.setMatricule(m);e.setNom(m);e.setPrenom(m);e.setEmail(m+"@x.fr");e.setUtilisateur(u);return teachers.saveAndFlush(e);} private Responsable guardianProfile(Utilisateur u){Responsable r=new Responsable();r.setNom("R");r.setPrenom(u.getEmailNormalise());r.setEmail(u.getEmailNormalise());r.setUtilisateur(u);return guardians.saveAndFlush(r);} private Eleve pupil(String n){Eleve e=new Eleve();e.setNumeroDossier(n);e.setNom(n);e.setPrenom(n);e.setDateNaissance(LocalDate.now().minusYears(12));return pupils.saveAndFlush(e);} private Classe classe(String c){return classe(c,"2025-2026");} private Classe classe(String c,String year){Classe x=new Classe();x.setCode(c);x.setNom(c);x.setNiveau("6e");x.setAnneeScolaire(year);return classes.saveAndFlush(x);} private Inscription registration(Eleve e,Classe c){return registration(e,c,StatutInscription.EN_COURS);} private Inscription registration(Eleve e,Classe c,StatutInscription status){Inscription i=new Inscription();i.setEleve(e);i.setClasse(c);i.setAnneeScolaire(c.getAnneeScolaire());i.setDateInscription(LocalDate.now());i.setStatut(status);if(status!=StatutInscription.EN_COURS)i.setDateFin(LocalDate.now());return registrations.saveAndFlush(i);} private Enseignement teaching(Enseignant t,Classe c){Enseignement e=new Enseignement();e.setEnseignant(t);e.setClasse(c);Matiere m=new Matiere();m.setCode("M"+c.getCode()+t.getMatricule());m.setNom("Math"+c.getCode()+t.getMatricule());m.setCoefficientDefaut(BigDecimal.ONE);m.setActif(true); e.setMatiere(matieres.saveAndFlush(m));e.setAnneeScolaire(c.getAnneeScolaire());e.setCoefficientMatiere(BigDecimal.ONE);return teachings.saveAndFlush(e);} private Note note(Inscription i,Enseignement e){Note n=new Note();n.setInscription(i);n.setEnseignement(e);n.setPeriode(PeriodeBulletin.TRIMESTRE_1);n.setValeur(BigDecimal.TEN);n.setBareme(new BigDecimal("20"));n.setCoefficient(BigDecimal.ONE);n.setDateEvaluation(LocalDate.now());n.setLibelle("x");return notes.saveAndFlush(n);} private void link(Responsable r,Eleve e){EleveResponsable l=new EleveResponsable();l.setResponsable(r);l.setEleve(e);l.setLienParente(LienParente.PERE);l.setValidFrom(LocalDate.now());links.saveAndFlush(l);} private Bulletin bulletin(Inscription i,StatutBulletin s){return bulletin(i,s,PeriodeBulletin.TRIMESTRE_1);} private Bulletin bulletin(Inscription i,StatutBulletin s,PeriodeBulletin p){Bulletin b=new Bulletin();b.setInscription(i);b.setPeriode(p);b.setDateGeneration(LocalDateTime.now());b.setStatut(s);b.setMoyenneGenerale(BigDecimal.TEN); BulletinLigne l=new BulletinLigne();l.setCodeMatiere("M");l.setNomMatiere("Math");l.setMoyenne(BigDecimal.TEN);l.setCoefficient(BigDecimal.ONE);l.setNombreNotes(1);b.ajouterLigne(l);return bulletins.saveAndFlush(b);} private String bearer(Utilisateur u){JwtClaimsSet c=JwtClaimsSet.builder().subject(u.getId().toString()).issuer("gestion-eleves-api-test").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).id(UUID.randomUUID().toString()).claim("role",u.getRole().name()).claim("ver",u.getTokenVersion()).build();return "Bearer "+encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),c)).getTokenValue();}
}
