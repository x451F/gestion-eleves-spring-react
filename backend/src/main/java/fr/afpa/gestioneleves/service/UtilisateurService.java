package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.response.UtilisateurResponse;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UtilisateurService {
    private final UtilisateurRepository repository;
    private final DomainMapper mapper;
    public UtilisateurService(UtilisateurRepository repository, DomainMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }
    public List<UtilisateurResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    public UtilisateurResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    public Optional<Utilisateur> trouverParEmailNormalise(String email) {
        return repository.findByEmailNormalise(email.trim().toLowerCase(Locale.ROOT));
    }
    public Utilisateur trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }
}
