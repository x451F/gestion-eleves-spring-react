package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.response.UtilisateurResponse;
import fr.afpa.gestioneleves.service.UtilisateurService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {
    private final UtilisateurService service;
    public UtilisateurController(UtilisateurService service) { this.service = service; }
    @GetMapping public List<UtilisateurResponse> lister() { return service.lister(); }
    @GetMapping("/{id}") public UtilisateurResponse obtenir(@PathVariable Long id) { return service.obtenir(id); }
}
