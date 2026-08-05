package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.InscriptionRequest;
import fr.afpa.gestioneleves.dto.response.*;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.service.BulletinService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import fr.afpa.gestioneleves.service.InscriptionService;
import fr.afpa.gestioneleves.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionController {
    private final InscriptionService service;
    private final NoteService noteService;
    private final BulletinService bulletinService;
    private final AccessPolicyService access;
    public InscriptionController(InscriptionService service, NoteService noteService, BulletinService bulletinService, AccessPolicyService access) {
        this.service = service; this.noteService = noteService; this.bulletinService = bulletinService; this.access = access;
    }
    @PostMapping public ResponseEntity<InscriptionResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody InscriptionRequest r) {
        access.admin(actor);
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/inscriptions/" + result.id())).body(result);
    }
    @GetMapping public List<InscriptionResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public InscriptionResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.inscription(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public InscriptionResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody InscriptionRequest r) { access.admin(actor); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/notes") public List<NoteResponse> notes(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.inscription(actor, id); access.teacherOrAdmin(actor); return actor != null && actor.role() == fr.afpa.gestioneleves.enumtype.Role.ENSEIGNANT ? noteService.parInscriptionPourEnseignant(id, actor) : noteService.parInscription(id); }
    @GetMapping("/{id}/moyennes") public MoyennesResponse moyennes(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @RequestParam PeriodeBulletin periode) { access.inscription(actor, id); access.teacherOrAdmin(actor); return actor != null && actor.role() == fr.afpa.gestioneleves.enumtype.Role.ENSEIGNANT ? noteService.moyennesPourEnseignant(id, periode, actor) : noteService.moyennes(id, periode); }
    @GetMapping("/{id}/bulletins") public List<BulletinResponse> bulletins(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.inscription(actor, id); access.teacherOrAdmin(actor); return actor != null && actor.role() == fr.afpa.gestioneleves.enumtype.Role.ENSEIGNANT ? bulletinService.parInscriptionPourEnseignant(id, actor) : bulletinService.parInscription(id); }
}
