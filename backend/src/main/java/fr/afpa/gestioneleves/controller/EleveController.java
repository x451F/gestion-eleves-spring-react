package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.EleveRequest;
import fr.afpa.gestioneleves.dto.response.*;
import fr.afpa.gestioneleves.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/eleves")
public class EleveController {
    private final EleveService service;
    private final InscriptionService inscriptionService;
    private final NoteService noteService;
    private final ResponsableService responsableService;

    public EleveController(EleveService service, InscriptionService inscriptionService,
                           NoteService noteService, ResponsableService responsableService) {
        this.service = service; this.inscriptionService = inscriptionService;
        this.noteService = noteService; this.responsableService = responsableService;
    }
    @PostMapping
    public ResponseEntity<EleveResponse> creer(@Valid @RequestBody EleveRequest request) {
        var result = service.creer(request);
        return ResponseEntity.created(URI.create("/api/eleves/" + result.id())).body(result);
    }
    @GetMapping public List<EleveResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public EleveResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public EleveResponse modifier(@PathVariable Long id, @Valid @RequestBody EleveRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/inscriptions") public List<InscriptionResponse> inscriptions(@PathVariable Long id) { return inscriptionService.parEleve(id); }
    @GetMapping("/{id}/notes") public List<NoteResponse> notes(@PathVariable Long id) { service.trouver(id); return noteService.parEleve(id); }
    @GetMapping("/{id}/responsables") public List<EleveResponsableResponse> responsables(@PathVariable Long id) { return responsableService.responsables(id); }
}
