package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.EleveRequest;
import fr.afpa.gestioneleves.dto.response.*;
import fr.afpa.gestioneleves.service.*;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AccessPolicyService access;

    public EleveController(EleveService service, InscriptionService inscriptionService,
                           NoteService noteService, ResponsableService responsableService, AccessPolicyService access) {
        this.service = service; this.inscriptionService = inscriptionService;
        this.noteService = noteService; this.responsableService = responsableService; this.access = access;
    }
    @PostMapping
    public ResponseEntity<EleveResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody EleveRequest request) {
        access.admin(actor);
        var result = service.creer(request);
        return ResponseEntity.created(URI.create("/api/eleves/" + result.id())).body(result);
    }
    @GetMapping public List<EleveResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { return service.listerVisible(actor); }
    @GetMapping("/{id}") public EleveResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.eleve(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public EleveResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody EleveRequest r) { access.admin(actor); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/inscriptions") public List<InscriptionResponse> inscriptions(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.eleve(actor, id); return actor != null && actor.role() == fr.afpa.gestioneleves.enumtype.Role.ENSEIGNANT ? inscriptionService.parElevePourEnseignant(id, actor) : inscriptionService.parEleve(id); }
    @GetMapping("/{id}/notes") public List<NoteResponse> notes(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.eleve(actor, id); access.teacherOrAdmin(actor); return actor != null && actor.role() == fr.afpa.gestioneleves.enumtype.Role.ENSEIGNANT ? noteService.parElevePourEnseignant(id, actor) : noteService.parEleve(id); }
    @GetMapping("/{id}/responsables") public List<EleveResponsableResponse> responsables(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); return responsableService.responsables(id); }
}
