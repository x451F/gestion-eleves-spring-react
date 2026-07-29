package fr.afpa.gestioneleves.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import fr.afpa.gestioneleves.dto.response.BulletinResponse;
import fr.afpa.gestioneleves.exception.FileStorageException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class BulletinPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generer(BulletinResponse bulletin) {
        if (bulletin.lignes() == null || bulletin.lignes().isEmpty()) {
            throw new FileStorageException("Le bulletin ne contient aucune donnée académique");
        }
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, sortie);
            document.open();

            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Établissement scolaire - Gestion des élèves", titre));
            document.add(new Paragraph("Bulletin scolaire", sousTitre));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Élève : " + bulletin.eleveNomComplet()));
            document.add(new Paragraph("Classe : " + bulletin.classeNom()));
            document.add(new Paragraph("Année scolaire : " + bulletin.anneeScolaire()));
            document.add(new Paragraph("Période : " + bulletin.periode()));
            document.add(new Paragraph("Généré le : " + DATE.format(bulletin.dateGeneration())));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{3f, 1.3f, 1.3f, 1.3f});
            table.setWidthPercentage(100);
            ajouterEntete(table, "Matière");
            ajouterEntete(table, "Moyenne");
            ajouterEntete(table, "Coefficient");
            ajouterEntete(table, "Notes");
            bulletin.lignes().forEach(ligne -> {
                table.addCell(ligne.nomMatiere());
                table.addCell(ligne.moyenne().toPlainString() + " / 20");
                table.addCell(ligne.coefficient().toPlainString());
                table.addCell(Integer.toString(ligne.nombreNotes()));
            });
            document.add(table);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Moyenne générale : " + bulletin.moyenneGenerale().toPlainString() + " / 20", sousTitre));
            if (bulletin.appreciation() != null && !bulletin.appreciation().isBlank()) {
                document.add(new Paragraph("Appréciation : " + bulletin.appreciation()));
            }
            document.close();
            return sortie.toByteArray();
        } catch (Exception ex) {
            throw new FileStorageException("Impossible de générer le bulletin PDF", ex);
        }
    }

    private void ajouterEntete(PdfPTable table, String texte) {
        var cellule = new com.lowagie.text.pdf.PdfPCell(
                new Phrase(texte, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cellule.setBackgroundColor(new java.awt.Color(226, 232, 240));
        cellule.setPadding(6);
        table.addCell(cellule);
    }
}
