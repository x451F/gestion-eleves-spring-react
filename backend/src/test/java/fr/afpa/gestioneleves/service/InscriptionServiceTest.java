package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.InscriptionRequest;
import fr.afpa.gestioneleves.entity.Classe;
import fr.afpa.gestioneleves.entity.Eleve;
import fr.afpa.gestioneleves.entity.Inscription;
import fr.afpa.gestioneleves.enumtype.StatutInscription;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.InscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {
    @Mock InscriptionRepository repository;
    @Mock EleveService eleveService;
    @Mock ClasseService classeService;
    private InscriptionService service;
    private InscriptionRequest request;
    private Classe classe;

    @BeforeEach void setUp() {
        service = new InscriptionService(repository, eleveService, classeService, new DomainMapper());
        request = new InscriptionRequest(1L, 2L, "2026-2027", LocalDate.of(2026, 9, 1), null, StatutInscription.ACTIVE);
        classe = new Classe(); classe.setNom("6e A"); classe.setAnneeScolaire("2026-2027");
    }
    @Test void creerSucces() {
        when(eleveService.trouver(1L)).thenReturn(new Eleve()); when(classeService.trouver(2L)).thenReturn(classe);
        when(repository.save(any(Inscription.class))).thenAnswer(i -> i.getArgument(0));
        assertThat(service.creer(request).anneeScolaire()).isEqualTo("2026-2027");
    }
    @Test void eleveAbsent() {
        when(eleveService.trouver(1L)).thenThrow(new ResourceNotFoundException("absent"));
        assertThatThrownBy(() -> service.creer(request)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void classeAbsente() {
        when(eleveService.trouver(1L)).thenReturn(new Eleve());
        when(classeService.trouver(2L)).thenThrow(new ResourceNotFoundException("absente"));
        assertThatThrownBy(() -> service.creer(request)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void inscriptionActiveDupliquee() {
        when(eleveService.trouver(1L)).thenReturn(new Eleve()); when(classeService.trouver(2L)).thenReturn(classe);
        when(repository.existsByEleveIdAndAnneeScolaireAndStatut(1L, "2026-2027", StatutInscription.ACTIVE)).thenReturn(true);
        assertThatThrownBy(() -> service.creer(request)).isInstanceOf(DuplicateResourceException.class);
    }
    @Test void anneeIncompatible() {
        classe.setAnneeScolaire("2025-2026");
        when(eleveService.trouver(1L)).thenReturn(new Eleve()); when(classeService.trouver(2L)).thenReturn(classe);
        assertThatThrownBy(() -> service.creer(request)).isInstanceOf(BusinessRuleException.class);
    }
}
