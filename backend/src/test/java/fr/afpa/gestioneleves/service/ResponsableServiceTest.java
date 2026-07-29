package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.AssociationResponsableRequest;
import fr.afpa.gestioneleves.entity.Eleve;
import fr.afpa.gestioneleves.entity.EleveResponsable;
import fr.afpa.gestioneleves.entity.Responsable;
import fr.afpa.gestioneleves.enumtype.LienParente;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.EleveResponsableRepository;
import fr.afpa.gestioneleves.repository.ResponsableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponsableServiceTest {
    @Mock ResponsableRepository repository;
    @Mock EleveResponsableRepository lienRepository;
    @Mock EleveService eleveService;
    private ResponsableService service;
    private final AssociationResponsableRequest request = new AssociationResponsableRequest(LienParente.MERE, true, true, true);

    @BeforeEach void setUp() { service = new ResponsableService(repository, lienRepository, eleveService, new DomainMapper()); }
    @Test void associationSucces() {
        Responsable r = new Responsable(); r.setNom("Martin"); r.setPrenom("Louise");
        Eleve e = new Eleve(); e.setNom("Martin"); e.setPrenom("Léo");
        when(repository.findById(1L)).thenReturn(Optional.of(r)); when(eleveService.trouver(2L)).thenReturn(e);
        when(lienRepository.save(any(EleveResponsable.class))).thenAnswer(i -> i.getArgument(0));
        assertThat(service.associer(1L, 2L, request).lienParente()).isEqualTo(LienParente.MERE);
    }
    @Test void associationDupliquee() {
        when(lienRepository.existsByEleveIdAndResponsableId(2L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.associer(1L, 2L, request)).isInstanceOf(DuplicateResourceException.class);
    }
    @Test void eleveAbsent() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Responsable()));
        when(eleveService.trouver(2L)).thenThrow(new ResourceNotFoundException("absent"));
        assertThatThrownBy(() -> service.associer(1L, 2L, request)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void responsableAbsent() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.associer(1L, 2L, request)).isInstanceOf(ResourceNotFoundException.class);
    }
}
