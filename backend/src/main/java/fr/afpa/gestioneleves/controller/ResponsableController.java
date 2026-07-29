package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.AssociationResponsableRequest;
import fr.afpa.gestioneleves.dto.request.ResponsableRequest;
import fr.afpa.gestioneleves.dto.response.EleveResponsableResponse;
import fr.afpa.gestioneleves.dto.response.ResponsableResponse;
import fr.afpa.gestioneleves.service.ResponsableService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/responsables")
public class ResponsableController {
    private final ResponsableService service;
    public ResponsableController(ResponsableService service) { this.service = service; }
    @PostMapping public ResponseEntity<ResponsableResponse> creer(@Valid @RequestBody ResponsableRequest r) {
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/responsables/" + result.id())).body(result);
    }
    @GetMapping public List<ResponsableResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public ResponsableResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public ResponsableResponse modifier(@PathVariable Long id, @Valid @RequestBody ResponsableRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{responsableId}/eleves/{eleveId}")
    public ResponseEntity<EleveResponsableResponse> associer(@PathVariable Long responsableId, @PathVariable Long eleveId,
                                                             @Valid @RequestBody AssociationResponsableRequest r) {
        var result = service.associer(responsableId, eleveId, r);
        return ResponseEntity.created(URI.create("/api/responsables/" + responsableId + "/eleves")).body(result);
    }
    @DeleteMapping("/{responsableId}/eleves/{eleveId}")
    public ResponseEntity<Void> dissocier(@PathVariable Long responsableId, @PathVariable Long eleveId) {
        service.dissocier(responsableId, eleveId); return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/eleves") public List<EleveResponsableResponse> eleves(@PathVariable Long id) { return service.eleves(id); }
}
