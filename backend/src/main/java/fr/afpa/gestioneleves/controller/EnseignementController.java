package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.EnseignementRequest;
import fr.afpa.gestioneleves.dto.response.EnseignementResponse;
import fr.afpa.gestioneleves.service.EnseignementService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/enseignements")
public class EnseignementController {
    private final EnseignementService service;
    private final AccessPolicyService access;
    public EnseignementController(EnseignementService service, AccessPolicyService access) { this.service = service; this.access = access; }
    @PostMapping public ResponseEntity<EnseignementResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody EnseignementRequest r) {
        access.admin(actor);
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/enseignements/" + result.id())).body(result);
    }
    @GetMapping public List<EnseignementResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public EnseignementResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.teaching(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public EnseignementResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody EnseignementRequest r) { access.admin(actor); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
}
