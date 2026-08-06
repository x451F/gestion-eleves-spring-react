package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.ClasseRequest;
import fr.afpa.gestioneleves.dto.response.ClasseResponse;
import fr.afpa.gestioneleves.dto.response.EnseignementResponse;
import fr.afpa.gestioneleves.dto.response.InscriptionResponse;
import fr.afpa.gestioneleves.service.ClasseService;
import fr.afpa.gestioneleves.service.EnseignementService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import fr.afpa.gestioneleves.service.InscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClasseController {
    private final ClasseService service;
    private final InscriptionService inscriptionService;
    private final EnseignementService enseignementService;
    private final AccessPolicyService access;
    public ClasseController(ClasseService service, InscriptionService inscriptionService, EnseignementService enseignementService, AccessPolicyService access) {
        this.service = service; this.inscriptionService = inscriptionService; this.enseignementService = enseignementService; this.access = access;
    }
    @PostMapping public ResponseEntity<ClasseResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody ClasseRequest r) {
        access.admin(actor);
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/classes/" + result.id())).body(result);
    }
    @GetMapping public List<ClasseResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public ClasseResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.classe(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public ClasseResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody ClasseRequest r) { access.admin(actor); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/inscriptions") public List<InscriptionResponse> inscriptions(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.classe(actor, id); return inscriptionService.parClasse(id); }
    @GetMapping("/{id}/enseignements") public List<EnseignementResponse> enseignements(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        access.classe(actor, id);
        return actor.role() == fr.afpa.gestioneleves.enumtype.Role.ADMIN
                ? enseignementService.parClasse(id)
                : enseignementService.parClassePourEnseignant(id, actor.id());
    }
}
