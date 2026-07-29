package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.ClasseRequest;
import fr.afpa.gestioneleves.dto.response.ClasseResponse;
import fr.afpa.gestioneleves.dto.response.EnseignementResponse;
import fr.afpa.gestioneleves.dto.response.InscriptionResponse;
import fr.afpa.gestioneleves.service.ClasseService;
import fr.afpa.gestioneleves.service.EnseignementService;
import fr.afpa.gestioneleves.service.InscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClasseController {
    private final ClasseService service;
    private final InscriptionService inscriptionService;
    private final EnseignementService enseignementService;
    public ClasseController(ClasseService service, InscriptionService inscriptionService, EnseignementService enseignementService) {
        this.service = service; this.inscriptionService = inscriptionService; this.enseignementService = enseignementService;
    }
    @PostMapping public ResponseEntity<ClasseResponse> creer(@Valid @RequestBody ClasseRequest r) {
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/classes/" + result.id())).body(result);
    }
    @GetMapping public List<ClasseResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public ClasseResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public ClasseResponse modifier(@PathVariable Long id, @Valid @RequestBody ClasseRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/inscriptions") public List<InscriptionResponse> inscriptions(@PathVariable Long id) { return inscriptionService.parClasse(id); }
    @GetMapping("/{id}/enseignements") public List<EnseignementResponse> enseignements(@PathVariable Long id) { return enseignementService.parClasse(id); }
}
