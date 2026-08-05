package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.AssociationResponsableRequest;
import fr.afpa.gestioneleves.dto.request.ResponsableRequest;
import fr.afpa.gestioneleves.dto.response.EleveResponsableResponse;
import fr.afpa.gestioneleves.dto.response.ResponsableResponse;
import fr.afpa.gestioneleves.service.ResponsableService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/responsables")
public class ResponsableController {
    private final ResponsableService service;
    private final AccessPolicyService access;
    public ResponsableController(ResponsableService service, AccessPolicyService access) { this.service = service; this.access = access; }
    @PostMapping public ResponseEntity<ResponsableResponse> creer(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody ResponsableRequest r) {
        access.admin(actor);
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/responsables/" + result.id())).body(result);
    }
    @GetMapping public List<ResponsableResponse> lister(@AuthenticationPrincipal AuthenticatedUser actor) { access.admin(actor); return service.lister(); }
    @GetMapping("/{id}") public ResponsableResponse obtenir(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.guardianProfile(actor, id); return service.obtenir(id); }
    @PutMapping("/{id}") public ResponsableResponse modifier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody ResponsableRequest r) { access.admin(actor); return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.admin(actor); service.supprimer(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{responsableId}/eleves/{eleveId}")
    public ResponseEntity<EleveResponsableResponse> associer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long responsableId, @PathVariable Long eleveId,
                                                             @Valid @RequestBody AssociationResponsableRequest r) {
        access.admin(actor); var result = service.associer(responsableId, eleveId, r);
        return ResponseEntity.created(URI.create("/api/responsables/" + responsableId + "/eleves")).body(result);
    }
    @DeleteMapping("/{responsableId}/eleves/{eleveId}")
    public ResponseEntity<Void> dissocier(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long responsableId, @PathVariable Long eleveId) {
        access.admin(actor); service.dissocier(responsableId, eleveId); return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/eleves") public List<EleveResponsableResponse> eleves(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { access.guardianProfile(actor, id); return actor != null && actor.role() == fr.afpa.gestioneleves.enumtype.Role.RESPONSABLE ? service.elevesActifs(actor) : service.eleves(id); }
}
