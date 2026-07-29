package fr.afpa.gestioneleves.pdf;

import fr.afpa.gestioneleves.dto.response.BulletinResponse;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.enumtype.StatutBulletin;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BulletinPdfServiceTest {
    private final BulletinPdfService service = new BulletinPdfService();

    @Test void produitUnPdfNonVideAvecSignature() {
        byte[] pdf = service.generer(bulletin());
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
    @Test void produitUnPdfDeTailleCredible() {
        assertThat(service.generer(bulletin()).length).isGreaterThan(500);
    }
    private BulletinResponse bulletin() {
        return new BulletinResponse(1L, 1L, "Dupont Alice", "6e A", "2026-2027",
                PeriodeBulletin.TRIMESTRE_1, LocalDateTime.now(), StatutBulletin.BROUILLON,
                new BigDecimal("15.50"), "Bon travail",
                List.of(new BulletinResponse.Ligne("MATH", "Mathématiques", new BigDecimal("15.50"), new BigDecimal("2"), 2)));
    }
}
