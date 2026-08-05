package fr.afpa.gestioneleves.integration;

import fr.afpa.gestioneleves.dto.request.*;
import fr.afpa.gestioneleves.entity.*;
import fr.afpa.gestioneleves.enumtype.*;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.repository.*;
import fr.afpa.gestioneleves.service.InscriptionService;
import fr.afpa.gestioneleves.service.ResponsableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Testcontainers @Transactional
class AcademicLifecycleIntegrationTest {
 @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("lifecycle").withUsername("test").withPassword("test");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);}
 @Autowired InscriptionService inscriptions; @Autowired ResponsableService responsables; @Autowired EleveRepository eleves; @Autowired ClasseRepository classes; @Autowired InscriptionRepository registrationRepo; @Autowired ResponsableRepository guardianRepo; @Autowired EleveResponsableRepository links;
 @Test void transferRetainsHistoryAndRejectsSameClass(){ Eleve e=eleve("E1"); Classe a=classe("A"),b=classe("B"); Inscription old=registration(e,a); var result=inscriptions.transferer(old.getId(),new TransferInscriptionRequest(b.getId(),LocalDate.now())); assertThat(registrationRepo.findById(old.getId()).orElseThrow().getStatut()).isEqualTo(StatutInscription.TERMINEE); assertThat(result.statut()).isEqualTo(StatutInscription.EN_COURS); assertThat(registrationRepo.findByEleveIdOrderByAnneeScolaireDesc(e.getId())).hasSize(2); assertThatThrownBy(()->inscriptions.transferer(result.id(),new TransferInscriptionRequest(b.getId(),LocalDate.now()))).isInstanceOf(BusinessRuleException.class); }
 @Test void endingLinkRetainsHistoryAndPrincipalChangeLeavesOneActivePrincipal(){ Eleve e=eleve("E2"); Responsable a=guardian("a"),b=guardian("b"); EleveResponsable la=link(e,a,true),lb=link(e,b,false); responsables.definirPrincipal(b.getId(),e.getId()); assertThat(links.findByEleveId(e.getId()).stream().filter(l->l.isResponsablePrincipal()&&l.getValidTo()==null)).hasSize(1).first().isSameAs(lb); responsables.dissocier(b.getId(),e.getId()); EleveResponsable ended=links.findByEleveIdAndResponsableId(e.getId(),b.getId()).orElseThrow(); assertThat(ended.getValidTo()).isEqualTo(LocalDate.now()); assertThat(links.findByEleveId(e.getId())).hasSize(2); }
 private Eleve eleve(String n){Eleve e=new Eleve();e.setNumeroDossier(n);e.setNom(n);e.setPrenom(n);e.setDateNaissance(LocalDate.now().minusYears(12));return eleves.saveAndFlush(e);} private Classe classe(String n){Classe c=new Classe();c.setCode(n);c.setNom(n);c.setNiveau("6e");c.setAnneeScolaire("2025-2026");return classes.saveAndFlush(c);} private Inscription registration(Eleve e,Classe c){Inscription i=new Inscription();i.setEleve(e);i.setClasse(c);i.setAnneeScolaire(c.getAnneeScolaire());i.setDateInscription(LocalDate.now());i.setStatut(StatutInscription.EN_COURS);return registrationRepo.saveAndFlush(i);} private Responsable guardian(String n){Responsable r=new Responsable();r.setNom(n);r.setPrenom(n);r.setEmail(n+"@x.fr");return guardianRepo.saveAndFlush(r);} private EleveResponsable link(Eleve e,Responsable r,boolean p){EleveResponsable l=new EleveResponsable();l.setEleve(e);l.setResponsable(r);l.setLienParente(LienParente.PERE);l.setValidFrom(LocalDate.now());l.setResponsablePrincipal(p);return links.saveAndFlush(l);}
}
