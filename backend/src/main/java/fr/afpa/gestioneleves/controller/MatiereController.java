package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.MatiereRequest;
import fr.afpa.gestioneleves.dto.response.MatiereResponse;
import fr.afpa.gestioneleves.service.MatiereService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/matieres")
@PreAuthorize("hasRole('ADMIN')")
public class MatiereController {
    private final MatiereService service;
    public MatiereController(MatiereService service) { this.service = service; }
    @PostMapping public ResponseEntity<MatiereResponse> creer(@Valid @RequestBody MatiereRequest r) {
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/matieres/" + result.id())).body(result);
    }
    @GetMapping public List<MatiereResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public MatiereResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public MatiereResponse modifier(@PathVariable Long id, @Valid @RequestBody MatiereRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
}
