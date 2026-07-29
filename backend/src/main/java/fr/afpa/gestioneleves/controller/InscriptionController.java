package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.InscriptionRequest;
import fr.afpa.gestioneleves.dto.response.*;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.service.BulletinService;
import fr.afpa.gestioneleves.service.InscriptionService;
import fr.afpa.gestioneleves.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionController {
    private final InscriptionService service;
    private final NoteService noteService;
    private final BulletinService bulletinService;
    public InscriptionController(InscriptionService service, NoteService noteService, BulletinService bulletinService) {
        this.service = service; this.noteService = noteService; this.bulletinService = bulletinService;
    }
    @PostMapping public ResponseEntity<InscriptionResponse> creer(@Valid @RequestBody InscriptionRequest r) {
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/inscriptions/" + result.id())).body(result);
    }
    @GetMapping public List<InscriptionResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public InscriptionResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public InscriptionResponse modifier(@PathVariable Long id, @Valid @RequestBody InscriptionRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/notes") public List<NoteResponse> notes(@PathVariable Long id) { return noteService.parInscription(id); }
    @GetMapping("/{id}/moyennes") public MoyennesResponse moyennes(@PathVariable Long id, @RequestParam PeriodeBulletin periode) { return noteService.moyennes(id, periode); }
    @GetMapping("/{id}/bulletins") public List<BulletinResponse> bulletins(@PathVariable Long id) { return bulletinService.parInscription(id); }
}
