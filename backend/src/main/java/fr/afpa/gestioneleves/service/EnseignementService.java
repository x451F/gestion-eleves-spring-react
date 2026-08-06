package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.EnseignementRequest;
import fr.afpa.gestioneleves.dto.response.EnseignementResponse;
import fr.afpa.gestioneleves.entity.Enseignement;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.EnseignementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnseignementService {
    private final EnseignementRepository repository;
    private final EnseignantService enseignantService;
    private final MatiereService matiereService;
    private final ClasseService classeService;
    private final DomainMapper mapper;

    public EnseignementService(EnseignementRepository repository, EnseignantService enseignantService,
                               MatiereService matiereService, ClasseService classeService, DomainMapper mapper) {
        this.repository = repository; this.enseignantService = enseignantService; this.matiereService = matiereService;
        this.classeService = classeService; this.mapper = mapper;
    }
    public EnseignementResponse creer(EnseignementRequest r) {
        verifierDoublon(r, null);
        var enseignant = enseignantService.trouver(r.enseignantId());
        var matiere = matiereService.trouver(r.matiereId());
        var classe = classeService.trouver(r.classeId());
        verifierAnnee(r, classe.getAnneeScolaire());
        Enseignement e = new Enseignement();
        e.setEnseignant(enseignant); e.setMatiere(matiere); e.setClasse(classe); appliquer(e, r);
        return mapper.toResponse(repository.save(e));
    }
    @Transactional(readOnly = true)
    public List<EnseignementResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public EnseignementResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    @Transactional(readOnly = true)
    public List<EnseignementResponse> parClasse(Long id) {
        classeService.trouver(id); return repository.findByClasseIdOrderByMatiereNom(id).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<EnseignementResponse> parClassePourEnseignant(Long id, Long utilisateurId) {
        classeService.trouver(id);
        return repository.findByClasseIdAndEnseignantUtilisateurIdOrderByMatiereNom(id, utilisateurId)
                .stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<EnseignementResponse> parEnseignant(Long id) {
        enseignantService.trouver(id); return repository.findByEnseignantIdOrderByAnneeScolaireDesc(id).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<EnseignementResponse> parUtilisateurEnseignant(Long utilisateurId) {
        return repository.findByEnseignantUtilisateurIdOrderByAnneeScolaireDesc(utilisateurId)
                .stream().map(mapper::toResponse).toList();
    }
    public EnseignementResponse modifier(Long id, EnseignementRequest r) {
        verifierDoublon(r, id);
        Enseignement e = trouver(id);
        var classe = classeService.trouver(r.classeId());
        verifierAnnee(r, classe.getAnneeScolaire());
        e.setEnseignant(enseignantService.trouver(r.enseignantId()));
        e.setMatiere(matiereService.trouver(r.matiereId())); e.setClasse(classe); appliquer(e, r);
        return mapper.toResponse(repository.save(e));
    }
    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }
    public Enseignement trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Enseignement introuvable : " + id));
    }
    private void verifierDoublon(EnseignementRequest r, Long id) {
        boolean duplicate = id == null
                ? repository.existsByEnseignantIdAndMatiereIdAndClasseIdAndAnneeScolaire(r.enseignantId(), r.matiereId(), r.classeId(), r.anneeScolaire())
                : repository.existsByEnseignantIdAndMatiereIdAndClasseIdAndAnneeScolaireAndIdNot(r.enseignantId(), r.matiereId(), r.classeId(), r.anneeScolaire(), id);
        if (duplicate) throw new DuplicateResourceException("Cet enseignement existe déjà");
    }
    private void verifierAnnee(EnseignementRequest r, String anneeClasse) {
        if (!anneeClasse.equals(r.anneeScolaire())) {
            throw new BusinessRuleException("L'année scolaire de l'enseignement doit correspondre à celle de la classe");
        }
    }
    private void appliquer(Enseignement e, EnseignementRequest r) {
        e.setAnneeScolaire(r.anneeScolaire()); e.setCoefficientMatiere(r.coefficientMatiere());
    }
}
