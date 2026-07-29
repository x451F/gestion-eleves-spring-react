package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.EleveRequest;
import fr.afpa.gestioneleves.entity.Eleve;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.EleveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EleveServiceTest {
    @Mock EleveRepository repository;
    private EleveService service;
    private EleveRequest request;

    @BeforeEach
    void setUp() {
        service = new EleveService(repository, new DomainMapper());
        request = new EleveRequest("D-001", "Dupont", "Alice", LocalDate.of(2012, 4, 2),
                "alice@example.test", null, true);
    }

    @Test void creerSucces() {
        when(repository.save(any(Eleve.class))).thenAnswer(i -> i.getArgument(0));
        var response = service.creer(request);
        assertThat(response.numeroDossier()).isEqualTo("D-001");
        verify(repository).save(any(Eleve.class));
    }
    @Test void creerRefuseNumeroDuplique() {
        when(repository.existsByNumeroDossierIgnoreCase("D-001")).thenReturn(true);
        assertThatThrownBy(() -> service.creer(request)).isInstanceOf(DuplicateResourceException.class);
    }
    @Test void obtenirSucces() {
        Eleve e = new Eleve(); e.setNumeroDossier("D-001"); e.setNom("Dupont"); e.setPrenom("Alice"); e.setDateNaissance(request.dateNaissance());
        when(repository.findById(1L)).thenReturn(Optional.of(e));
        assertThat(service.obtenir(1L).nom()).isEqualTo("Dupont");
    }
    @Test void obtenirAbsent() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenir(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void modifierSucces() {
        Eleve e = new Eleve(); when(repository.findById(1L)).thenReturn(Optional.of(e));
        when(repository.save(e)).thenReturn(e);
        assertThat(service.modifier(1L, request).prenom()).isEqualTo("Alice");
    }
    @Test void supprimerSucces() {
        Eleve e = new Eleve(); when(repository.findById(1L)).thenReturn(Optional.of(e));
        service.supprimer(1L);
        verify(repository).delete(e); verify(repository).flush();
    }
    @Test void listerRetourneLesEleves() {
        Eleve e = new Eleve(); e.setNumeroDossier("D-001"); e.setNom("Dupont"); e.setPrenom("Alice"); e.setDateNaissance(request.dateNaissance());
        when(repository.findAll()).thenReturn(List.of(e));
        assertThat(service.lister()).hasSize(1);
    }
}
