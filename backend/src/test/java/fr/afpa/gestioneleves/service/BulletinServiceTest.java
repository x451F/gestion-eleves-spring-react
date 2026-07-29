package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.BulletinGenerateRequest;
import fr.afpa.gestioneleves.dto.response.BulletinResponse;
import fr.afpa.gestioneleves.entity.*;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.enumtype.StatutBulletin;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.BulletinRepository;
import fr.afpa.gestioneleves.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulletinServiceTest {
    @Mock BulletinRepository repository;
    @Mock NoteRepository noteRepository;
    @Mock InscriptionService inscriptionService;
    @Mock DomainMapper mapper;
    private BulletinService service;
    private final BulletinGenerateRequest request = new BulletinGenerateRequest(1L, PeriodeBulletin.TRIMESTRE_1, "Bien");

    @BeforeEach void setUp() {
        service = new BulletinService(repository, noteRepository, inscriptionService, new CalculMoyenneService(), mapper);
    }
    @Test void generationEtMoyennePondereeCorrectes() {
        when(inscriptionService.trouver(1L)).thenReturn(mock(Inscription.class));
        List<Note> notes = List.of(
                note(1L, "MATH", "Mathématiques", "18", "1", "2"),
                note(2L, "FRA", "Français", "12", "1", "1"));
        when(noteRepository.findByInscriptionIdAndPeriode(1L, PeriodeBulletin.TRIMESTRE_1))
                .thenReturn(notes);
        when(repository.save(any(Bulletin.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(Bulletin.class))).thenReturn(mock(BulletinResponse.class));
        service.generer(request);
        ArgumentCaptor<Bulletin> captor = ArgumentCaptor.forClass(Bulletin.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMoyenneGenerale()).isEqualByComparingTo("16.00");
        assertThat(captor.getValue().getLignes()).hasSize(2);
    }
    @Test void doublonRefuse() {
        when(repository.existsByInscriptionIdAndPeriode(1L, PeriodeBulletin.TRIMESTRE_1)).thenReturn(true);
        assertThatThrownBy(() -> service.generer(request)).isInstanceOf(DuplicateResourceException.class);
    }
    @Test void absenceDeNotesRefusee() {
        when(inscriptionService.trouver(1L)).thenReturn(mock(Inscription.class));
        when(noteRepository.findByInscriptionIdAndPeriode(1L, PeriodeBulletin.TRIMESTRE_1)).thenReturn(List.of());
        assertThatThrownBy(() -> service.generer(request)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void publicationChangeLeStatut() {
        Bulletin b = new Bulletin(); b.setStatut(StatutBulletin.BROUILLON);
        when(repository.findById(1L)).thenReturn(Optional.of(b)); when(repository.save(b)).thenReturn(b);
        when(mapper.toResponse(b)).thenReturn(mock(BulletinResponse.class));
        service.publier(1L);
        assertThat(b.getStatut()).isEqualTo(StatutBulletin.PUBLIE);
    }
    @Test void suppressionBulletinPublieRefusee() {
        Bulletin b = new Bulletin(); b.setStatut(StatutBulletin.PUBLIE);
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.supprimer(1L)).isInstanceOf(BusinessRuleException.class);
    }
    private Note note(Long id, String code, String nom, String valeur, String coefNote, String coefMatiere) {
        Matiere m = mock(Matiere.class); when(m.getId()).thenReturn(id); when(m.getCode()).thenReturn(code); when(m.getNom()).thenReturn(nom);
        Enseignement e = mock(Enseignement.class); when(e.getMatiere()).thenReturn(m); when(e.getCoefficientMatiere()).thenReturn(new BigDecimal(coefMatiere));
        Note n = mock(Note.class); when(n.getEnseignement()).thenReturn(e); when(n.getValeur()).thenReturn(new BigDecimal(valeur)); when(n.getCoefficient()).thenReturn(new BigDecimal(coefNote));
        return n;
    }
}
