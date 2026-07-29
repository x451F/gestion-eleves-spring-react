package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.BulletinGenerateRequest;
import fr.afpa.gestioneleves.dto.response.BulletinResponse;
import fr.afpa.gestioneleves.pdf.BulletinPdfService;
import fr.afpa.gestioneleves.service.BulletinService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/bulletins")
public class BulletinController {
    private final BulletinService service;
    private final BulletinPdfService pdfService;
    public BulletinController(BulletinService service, BulletinPdfService pdfService) {
        this.service = service; this.pdfService = pdfService;
    }
    @PostMapping("/generate")
    public ResponseEntity<BulletinResponse> generer(@Valid @RequestBody BulletinGenerateRequest r) {
        var result = service.generer(r); return ResponseEntity.created(URI.create("/api/bulletins/" + result.id())).body(result);
    }
    @GetMapping public List<BulletinResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public BulletinResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PostMapping("/{id}/publier") public BulletinResponse publier(@PathVariable Long id) { return service.publier(id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        BulletinResponse bulletin = service.obtenir(id);
        byte[] pdf = pdfService.generer(bulletin);
        String nom = Normalizer.normalize(bulletin.eleveNomComplet(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String fichier = "bulletin-" + nom + "-" + bulletin.periode().name().toLowerCase(Locale.ROOT) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fichier, StandardCharsets.UTF_8).build().toString())
                .body(pdf);
    }
}
