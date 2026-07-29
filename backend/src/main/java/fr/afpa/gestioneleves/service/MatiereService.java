package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.MatiereRequest;
import fr.afpa.gestioneleves.dto.response.MatiereResponse;
import fr.afpa.gestioneleves.entity.Matiere;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.MatiereRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MatiereService {
    private final MatiereRepository repository;
    private final DomainMapper mapper;
    public MatiereService(MatiereRepository repository, DomainMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }
    public MatiereResponse creer(MatiereRequest r) {
        verifier(r, null); Matiere m = new Matiere(); appliquer(m, r); return mapper.toResponse(repository.save(m));
    }
    @Transactional(readOnly = true)
    public List<MatiereResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public MatiereResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    public MatiereResponse modifier(Long id, MatiereRequest r) {
        verifier(r, id); Matiere m = trouver(id); appliquer(m, r); return mapper.toResponse(repository.save(m));
    }
    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }
    public Matiere trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Matière introuvable : " + id));
    }
    private void verifier(MatiereRequest r, Long id) {
        boolean code = id == null ? repository.existsByCodeIgnoreCase(r.code())
                : repository.existsByCodeIgnoreCaseAndIdNot(r.code(), id);
        boolean nom = id == null ? repository.existsByNomIgnoreCase(r.nom())
                : repository.existsByNomIgnoreCaseAndIdNot(r.nom(), id);
        if (code || nom) throw new DuplicateResourceException("Cette matière existe déjà");
    }
    private void appliquer(Matiere m, MatiereRequest r) {
        m.setCode(r.code().trim().toUpperCase()); m.setNom(r.nom().trim());
        m.setCoefficientDefaut(r.coefficientDefaut()); m.setActif(r.actif() == null || r.actif());
    }
}
