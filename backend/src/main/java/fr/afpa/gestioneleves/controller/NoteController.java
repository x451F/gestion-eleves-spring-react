package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.request.NoteRequest;
import fr.afpa.gestioneleves.dto.response.NoteResponse;
import fr.afpa.gestioneleves.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;
    public NoteController(NoteService service) { this.service = service; }
    @PostMapping public ResponseEntity<NoteResponse> creer(@Valid @RequestBody NoteRequest r) {
        var result = service.creer(r); return ResponseEntity.created(URI.create("/api/notes/" + result.id())).body(result);
    }
    @GetMapping public List<NoteResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public NoteResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
    @PutMapping("/{id}") public NoteResponse modifier(@PathVariable Long id, @Valid @RequestBody NoteRequest r) { return service.modifier(id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> supprimer(@PathVariable Long id) { service.supprimer(id); return ResponseEntity.noContent().build(); }
}
