package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.EnseignantRequest;
import fr.afpa.gestioneleves.dto.response.EnseignantResponse;
import fr.afpa.gestioneleves.entity.Enseignant;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.EnseignantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnseignantService {
    private final EnseignantRepository repository;
    private final DomainMapper mapper;

    public EnseignantService(EnseignantRepository repository, DomainMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public EnseignantResponse creer(EnseignantRequest r) {
        verifier(r, null);
        Enseignant e = new Enseignant();
        appliquer(e, r);
        return mapper.toResponse(repository.save(e));
    }
    @Transactional(readOnly = true)
    public List<EnseignantResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public EnseignantResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    @Transactional(readOnly = true)
    public EnseignantResponse obtenirParUtilisateur(Long utilisateurId) {
        return mapper.toResponse(repository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil enseignant introuvable.")));
    }
    public EnseignantResponse modifier(Long id, EnseignantRequest r) {
        verifier(r, id); Enseignant e = trouver(id); appliquer(e, r); return mapper.toResponse(repository.save(e));
    }
    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }
    public Enseignant trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable : " + id));
    }
    private void verifier(EnseignantRequest r, Long id) {
        boolean matricule = id == null ? repository.existsByMatriculeIgnoreCase(r.matricule())
                : repository.existsByMatriculeIgnoreCaseAndIdNot(r.matricule(), id);
        boolean email = id == null ? repository.existsByEmailIgnoreCase(r.email())
                : repository.existsByEmailIgnoreCaseAndIdNot(r.email(), id);
        if (matricule || email) throw new DuplicateResourceException("Matricule ou email enseignant déjà utilisé");
    }
    private void appliquer(Enseignant e, EnseignantRequest r) {
        e.setMatricule(r.matricule().trim().toUpperCase()); e.setNom(r.nom().trim());
        e.setPrenom(r.prenom().trim()); e.setEmail(r.email().trim().toLowerCase());
        e.setActif(r.actif() == null || r.actif());
    }
}
