package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.EnseignementRequest;
import fr.afpa.gestioneleves.dto.response.EnseignementResponse;
import fr.afpa.gestioneleves.service.EnseignementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/enseignements")
public class EnseignementController {
    private final EnseignementService service;
    public EnseignementController(EnseignementService service) { this.service = service; }
    @PostMapping public ResponseEntity<EnseignementResponse> creer(@Valid @RequestBody EnseignementRequest r) {
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/enseignements/" + result.id())).body(result);
    }
    @GetMapping public List<EnseignementResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public EnseignementResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public EnseignementResponse modifier(@PathVariable Long id, @Valid @RequestBody EnseignementRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
}
