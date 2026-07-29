package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.entity.Enseignement;
import fr.afpa.gestioneleves.entity.Matiere;
import fr.afpa.gestioneleves.entity.Note;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CalculMoyenneServiceTest {
    private final CalculMoyenneService service = new CalculMoyenneService();

    @Test void calculeMoyenneMatierePonderee() {
        var result = service.calculer(1L, PeriodeBulletin.TRIMESTRE_1,
                List.of(note(1L, "Mathématiques", "MATH", "2", "10", "1"),
                        note(1L, "Mathématiques", "MATH", "2", "20", "3")));
        assertThat(result.matieres().getFirst().moyenne()).isEqualByComparingTo("17.50");
    }
    @Test void calculeMoyenneGeneralePonderee() {
        var result = service.calculer(1L, PeriodeBulletin.TRIMESTRE_1,
                List.of(note(1L, "Mathématiques", "MATH", "2", "18", "1"),
                        note(2L, "Français", "FRA", "1", "12", "1")));
        assertThat(result.moyenneGenerale()).isEqualByComparingTo("16.00");
    }
    @Test void appliqueArrondiHalfUp() {
        var result = service.calculer(1L, PeriodeBulletin.TRIMESTRE_1,
                List.of(note(1L, "Mathématiques", "MATH", "1", "10", "1"),
                        note(1L, "Mathématiques", "MATH", "1", "11", "2")));
        assertThat(result.moyenneGenerale()).isEqualByComparingTo("10.67");
    }
    @Test void sansNoteRetourneMoyenneAbsente() {
        var result = service.calculer(1L, PeriodeBulletin.TRIMESTRE_1, List.of());
        assertThat(result.moyenneGenerale()).isNull();
        assertThat(result.matieres()).isEmpty();
    }

    private Note note(Long matiereId, String nom, String code, String coefficientMatiere,
                      String valeur, String coefficientNote) {
        Matiere matiere = mock(Matiere.class);
        when(matiere.getId()).thenReturn(matiereId); when(matiere.getNom()).thenReturn(nom); when(matiere.getCode()).thenReturn(code);
        Enseignement enseignement = mock(Enseignement.class);
        when(enseignement.getMatiere()).thenReturn(matiere);
        when(enseignement.getCoefficientMatiere()).thenReturn(new BigDecimal(coefficientMatiere));
        Note note = mock(Note.class);
        when(note.getEnseignement()).thenReturn(enseignement);
        when(note.getValeur()).thenReturn(new BigDecimal(valeur));
        when(note.getCoefficient()).thenReturn(new BigDecimal(coefficientNote));
        return note;
    }
}
