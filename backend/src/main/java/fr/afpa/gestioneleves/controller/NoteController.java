package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.NoteRequest;
import fr.afpa.gestioneleves.dto.response.NoteResponse;
import fr.afpa.gestioneleves.service.NoteService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;
    private final AccessPolicyService access;
    public NoteController(NoteService service, AccessPolicyService access) { this.service = service; this.access = access; }
    @PostMapping public ResponseEntity<NoteResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody NoteRequest r) {
        access.writeNote(actor, r.enseignementId());
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/notes/" + result.id())).body(result);
    }
    @GetMapping public List<NoteResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public NoteResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.note(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public NoteResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody NoteRequest r) { access.note(actor, id); access.writeNote(actor, r.enseignementId()); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.note(actor, id); service.supprimer(id); return ResponseEntity.noContent().build(); }
}
