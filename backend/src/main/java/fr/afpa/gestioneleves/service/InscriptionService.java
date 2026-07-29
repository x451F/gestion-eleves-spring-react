package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.InscriptionRequest;
import fr.afpa.gestioneleves.dto.response.InscriptionResponse;
import fr.afpa.gestioneleves.entity.Inscription;
import fr.afpa.gestioneleves.enumtype.StatutInscription;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.InscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InscriptionService {
    private final InscriptionRepository repository;
    private final EleveService eleveService;
    private final ClasseService classeService;
    private final DomainMapper mapper;

    public InscriptionService(InscriptionRepository repository, EleveService eleveService,
                              ClasseService classeService, DomainMapper mapper) {
        this.repository = repository; this.eleveService = eleveService; this.classeService = classeService; this.mapper = mapper;
    }

    public InscriptionResponse creer(InscriptionRequest r) {
        var eleve = eleveService.trouver(r.eleveId());
        var classe = classeService.trouver(r.classeId());
        verifier(r, classe.getAnneeScolaire(), null);
        Inscription i = new Inscription();
        i.setEleve(eleve); i.setClasse(classe); appliquer(i, r);
        return mapper.toResponse(repository.save(i));
    }

    @Transactional(readOnly = true)
    public List<InscriptionResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public InscriptionResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    @Transactional(readOnly = true)
    public List<InscriptionResponse> parEleve(Long id) {
        eleveService.trouver(id); return repository.findByEleveIdOrderByAnneeScolaireDesc(id).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<InscriptionResponse> parClasse(Long id) {
        classeService.trouver(id); return repository.findByClasseIdOrderByAnneeScolaireDesc(id).stream().map(mapper::toResponse).toList();
    }
    public InscriptionResponse modifier(Long id, InscriptionRequest r) {
        Inscription i = trouver(id);
        var eleve = eleveService.trouver(r.eleveId());
        var classe = classeService.trouver(r.classeId());
        verifier(r, classe.getAnneeScolaire(), id);
        i.setEleve(eleve); i.setClasse(classe); appliquer(i, r);
        return mapper.toResponse(repository.save(i));
    }
    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }
    public Inscription trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable : " + id));
    }
    private void verifier(InscriptionRequest r, String anneeClasse, Long id) {
        if (!anneeClasse.equals(r.anneeScolaire())) {
            throw new BusinessRuleException("L'année scolaire de l'inscription doit correspondre à celle de la classe");
        }
        if (r.dateFin() != null && r.dateFin().isBefore(r.dateInscription())) {
            throw new BusinessRuleException("La date de fin ne peut pas précéder la date d'inscription");
        }
        if (r.statut() == StatutInscription.ACTIVE) {
            boolean duplicate = id == null
                    ? repository.existsByEleveIdAndAnneeScolaireAndStatut(r.eleveId(), r.anneeScolaire(), StatutInscription.ACTIVE)
                    : repository.existsByEleveIdAndAnneeScolaireAndStatutAndIdNot(r.eleveId(), r.anneeScolaire(), StatutInscription.ACTIVE, id);
            if (duplicate) throw new DuplicateResourceException("Une inscription active existe déjà pour cet élève et cette année");
        }
        if (r.statut() == StatutInscription.ACTIVE && r.dateFin() != null) {
            throw new BusinessRuleException("Une inscription active ne doit pas avoir de date de fin");
        }
    }
    private void appliquer(Inscription i, InscriptionRequest r) {
        i.setAnneeScolaire(r.anneeScolaire()); i.setDateInscription(r.dateInscription());
        i.setDateFin(r.dateFin()); i.setStatut(r.statut());
    }
}
