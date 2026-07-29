package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.NoteRequest;
import fr.afpa.gestioneleves.dto.response.NoteResponse;
import fr.afpa.gestioneleves.entity.*;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {
    @Mock NoteRepository repository;
    @Mock InscriptionService inscriptionService;
    @Mock EnseignementService enseignementService;
    @Mock DomainMapper mapper;
    private NoteService service;
    private Inscription inscription;
    private Enseignement enseignement;

    @BeforeEach void setUp() {
        service = new NoteService(repository, inscriptionService, enseignementService, new CalculMoyenneService(), mapper);
        Classe classe = mock(Classe.class); lenient().when(classe.getId()).thenReturn(10L);
        inscription = mock(Inscription.class); lenient().when(inscription.getClasse()).thenReturn(classe); lenient().when(inscription.getAnneeScolaire()).thenReturn("2026-2027");
        enseignement = mock(Enseignement.class); lenient().when(enseignement.getClasse()).thenReturn(classe); lenient().when(enseignement.getAnneeScolaire()).thenReturn("2026-2027");
    }
    @Test void creerSucces() {
        lenient().when(inscriptionService.trouver(1L)).thenReturn(inscription); lenient().when(enseignementService.trouver(2L)).thenReturn(enseignement);
        lenient().when(repository.save(any(Note.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(mapper.toResponse(any(Note.class))).thenReturn(mock(NoteResponse.class));
        assertThat(service.creer(request("15", "1"))).isNotNull();
    }
    @Test void valeurSousZero() {
        assertThatThrownBy(() -> service.creer(request("-0.01", "1"))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void valeurAuDessusDeVingt() {
        assertThatThrownBy(() -> service.creer(request("20.01", "1"))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void coefficientNonPositif() {
        assertThatThrownBy(() -> service.creer(request("15", "0"))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void inscriptionAbsente() {
        when(inscriptionService.trouver(1L)).thenThrow(new ResourceNotFoundException("absente"));
        assertThatThrownBy(() -> service.creer(request("15", "1"))).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void enseignementAbsent() {
        when(inscriptionService.trouver(1L)).thenReturn(inscription);
        when(enseignementService.trouver(2L)).thenThrow(new ResourceNotFoundException("absent"));
        assertThatThrownBy(() -> service.creer(request("15", "1"))).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void classeIncompatible() {
        Classe autre = mock(Classe.class); when(autre.getId()).thenReturn(99L);
        when(enseignement.getClasse()).thenReturn(autre);
        when(inscriptionService.trouver(1L)).thenReturn(inscription); when(enseignementService.trouver(2L)).thenReturn(enseignement);
        assertThatThrownBy(() -> service.creer(request("15", "1"))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void anneeIncompatible() {
        when(enseignement.getAnneeScolaire()).thenReturn("2025-2026");
        when(inscriptionService.trouver(1L)).thenReturn(inscription); when(enseignementService.trouver(2L)).thenReturn(enseignement);
        assertThatThrownBy(() -> service.creer(request("15", "1"))).isInstanceOf(BusinessRuleException.class);
    }
    private NoteRequest request(String valeur, String coefficient) {
        return new NoteRequest(1L, 2L, PeriodeBulletin.TRIMESTRE_1, new BigDecimal(valeur),
                new BigDecimal("20"), new BigDecimal(coefficient), LocalDate.now(), "Contrôle", null);
    }
}
