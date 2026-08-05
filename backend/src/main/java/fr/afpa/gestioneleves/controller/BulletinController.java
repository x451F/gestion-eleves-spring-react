package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.BulletinGenerateRequest;
import fr.afpa.gestioneleves.dto.response.BulletinResponse;
import fr.afpa.gestioneleves.pdf.BulletinPdfService;
import fr.afpa.gestioneleves.service.BulletinService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AccessPolicyService access;
    public BulletinController(BulletinService service, BulletinPdfService pdfService, AccessPolicyService access) {
        this.service = service; this.pdfService = pdfService; this.access = access;
    }
    @PostMapping("/generate")
    public ResponseEntity<BulletinResponse> generer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody BulletinGenerateRequest r) {
        access.admin(actor);
        var result = service.generer(r); return ResponseEntity.created(URI.create("/api/bulletins/" + result.id())).body(result);
    }
    @GetMapping public List<BulletinResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public BulletinResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.bulletin(actor, id); return service.obtenir(id); }
    @PostMapping("/{id}/publier") public BulletinResponse publier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); return service.publier(id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        access.bulletin(actor, id);
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
