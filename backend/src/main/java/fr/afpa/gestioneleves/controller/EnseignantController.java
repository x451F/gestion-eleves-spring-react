package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.EnseignantRequest;
import fr.afpa.gestioneleves.dto.response.EnseignantResponse;
import fr.afpa.gestioneleves.dto.response.EnseignementResponse;
import fr.afpa.gestioneleves.service.EnseignantService;
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
@RequestMapping("/api/enseignants")
public class EnseignantController {
    private final EnseignantService service;
    private final EnseignementService enseignementService;
    private final AccessPolicyService access;
    public EnseignantController(EnseignantService service, EnseignementService enseignementService, AccessPolicyService access) {
        this.service = service; this.enseignementService = enseignementService; this.access = access;
    }
    @PostMapping public ResponseEntity<EnseignantResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody EnseignantRequest r) {
        access.admin(actor);
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/enseignants/" + result.id())).body(result);
    }
    @GetMapping public List<EnseignantResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public EnseignantResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.teacherProfile(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public EnseignantResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody EnseignantRequest r) { access.admin(actor); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/enseignements") public List<EnseignementResponse> enseignements(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.teacherProfile(actor, id); return enseignementService.parEnseignant(id); }
}
