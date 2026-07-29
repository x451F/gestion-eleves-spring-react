package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.ClasseRequest;
import fr.afpa.gestioneleves.dto.response.ClasseResponse;
import fr.afpa.gestioneleves.entity.Classe;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.ClasseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClasseService {
    private final ClasseRepository repository;
    private final DomainMapper mapper;

    public ClasseService(ClasseRepository repository, DomainMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ClasseResponse creer(ClasseRequest request) {
        verifierDoublon(request, null);
        Classe classe = new Classe();
        appliquer(classe, request);
        return mapper.toResponse(repository.save(classe));
    }

    @Transactional(readOnly = true)
    public List<ClasseResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }

    @Transactional(readOnly = true)
    public ClasseResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }

    public ClasseResponse modifier(Long id, ClasseRequest request) {
        verifierDoublon(request, id);
        Classe classe = trouver(id);
        appliquer(classe, request);
        return mapper.toResponse(repository.save(classe));
    }

    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }

    public Classe trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Classe introuvable : " + id));
    }

    private void verifierDoublon(ClasseRequest r, Long id) {
        boolean doublonCode = id == null ? repository.existsByCodeIgnoreCase(r.code())
                : repository.existsByCodeIgnoreCaseAndIdNot(r.code(), id);
        boolean doublonNom = id == null ? repository.existsByNomIgnoreCaseAndAnneeScolaire(r.nom(), r.anneeScolaire())
                : repository.existsByNomIgnoreCaseAndAnneeScolaireAndIdNot(r.nom(), r.anneeScolaire(), id);
        if (doublonCode || doublonNom) throw new DuplicateResourceException("Cette classe existe déjà");
    }

    private void appliquer(Classe c, ClasseRequest r) {
        c.setCode(r.code().trim().toUpperCase());
        c.setNom(r.nom().trim());
        c.setNiveau(r.niveau().trim());
        c.setAnneeScolaire(r.anneeScolaire());
        c.setActif(r.actif() == null || r.actif());
    }
}
