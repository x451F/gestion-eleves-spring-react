package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.InscriptionRequest;
import fr.afpa.gestioneleves.dto.request.TransferInscriptionRequest;
import fr.afpa.gestioneleves.dto.request.DateFinRequest;
import fr.afpa.gestioneleves.dto.response.InscriptionResponse;
import fr.afpa.gestioneleves.entity.Inscription;
import fr.afpa.gestioneleves.enumtype.StatutInscription;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.InscriptionRepository;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
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
    public List<InscriptionResponse> parElevePourEnseignant(Long id, AuthenticatedUser actor) {
        return repository.findVisibleToTeacherByEleveId(id, actor.id()).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<InscriptionResponse> parClasse(Long id) {
        classeService.trouver(id); return repository.findByClasseIdOrderByAnneeScolaireDesc(id).stream().map(mapper::toResponse).toList();
    }
    public InscriptionResponse modifier(Long id, InscriptionRequest r) {
        Inscription i = trouverVerrouille(id);
        var eleve = eleveService.verrouiller(r.eleveId());
        var classe = classeService.trouver(r.classeId());
        if (i.getStatut() != StatutInscription.EN_COURS
                || !i.getEleve().getId().equals(eleve.getId())
                || !i.getClasse().getId().equals(classe.getId())
                || i.getStatut() != r.statut()) {
            throw new BusinessRuleException("Utilisez les opérations de cycle de vie pour modifier cette inscription");
        }
        verifier(r, classe.getAnneeScolaire(), id);
        i.setEleve(eleve); i.setClasse(classe); appliquer(i, r);
        return mapper.toResponse(repository.save(i));
    }
    public void supprimer(Long id) { throw new BusinessRuleException("Une inscription historique ne peut pas être supprimée"); }
    public InscriptionResponse transferer(Long inscriptionId, TransferInscriptionRequest request) {
        Inscription actuelle = trouverVerrouille(inscriptionId);
        var eleve = eleveService.verrouiller(actuelle.getEleve().getId());
        if (actuelle.getStatut() != StatutInscription.EN_COURS) throw new BusinessRuleException("Seule une inscription en cours peut être transférée");
        var destination = classeService.trouver(request.classeId());
        if (actuelle.getClasse().getId().equals(destination.getId())) throw new BusinessRuleException("Le transfert vers la même classe est interdit");
        if (request.dateTransfert().isBefore(actuelle.getDateInscription())) throw new BusinessRuleException("La date de transfert est invalide");
        actuelle.setStatut(StatutInscription.TERMINEE); actuelle.setDateFin(request.dateTransfert());
        repository.save(actuelle);
        repository.flush();
        Inscription nouvelle = new Inscription();
        nouvelle.setEleve(eleve); nouvelle.setClasse(destination); nouvelle.setAnneeScolaire(destination.getAnneeScolaire());
        nouvelle.setDateInscription(request.dateTransfert()); nouvelle.setStatut(StatutInscription.EN_COURS);
        return mapper.toResponse(repository.save(nouvelle));
    }
    public InscriptionResponse terminer(Long id, DateFinRequest request) { return cloturer(id, request.dateFin(), StatutInscription.TERMINEE); }
    public InscriptionResponse annuler(Long id, DateFinRequest request) { return cloturer(id, request.dateFin(), StatutInscription.ANNULEE); }
    public Inscription trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable : " + id));
    }
    private Inscription trouverVerrouille(Long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable : " + id));
    }
    private InscriptionResponse cloturer(Long id, java.time.LocalDate dateFin, StatutInscription destination) {
        Inscription inscription = trouverVerrouille(id);
        eleveService.verrouiller(inscription.getEleve().getId());
        if (dateFin.isBefore(inscription.getDateInscription())) throw new BusinessRuleException("La date de fin ne peut pas précéder la date d'inscription");
        if (inscription.getStatut() == destination) return mapper.toResponse(inscription);
        if (inscription.getStatut() != StatutInscription.EN_COURS) throw new BusinessRuleException("Cette inscription historique ne peut pas changer de statut");
        inscription.setStatut(destination); inscription.setDateFin(dateFin);
        return mapper.toResponse(repository.save(inscription));
    }
    private void verifier(InscriptionRequest r, String anneeClasse, Long id) {
        if (!anneeClasse.equals(r.anneeScolaire())) {
            throw new BusinessRuleException("L'année scolaire de l'inscription doit correspondre à celle de la classe");
        }
        if (r.dateFin() != null && r.dateFin().isBefore(r.dateInscription())) {
            throw new BusinessRuleException("La date de fin ne peut pas précéder la date d'inscription");
        }
        if (r.statut() == StatutInscription.EN_COURS) {
            boolean duplicate = id == null
                    ? repository.existsByEleveIdAndAnneeScolaireAndStatut(r.eleveId(), r.anneeScolaire(), StatutInscription.EN_COURS)
                    : repository.existsByEleveIdAndAnneeScolaireAndStatutAndIdNot(r.eleveId(), r.anneeScolaire(), StatutInscription.EN_COURS, id);
            if (duplicate) throw new DuplicateResourceException("Une inscription active existe déjà pour cet élève et cette année");
        }
        if (r.statut() == StatutInscription.EN_COURS && r.dateFin() != null) {
            throw new BusinessRuleException("Une inscription active ne doit pas avoir de date de fin");
        }
    }
    private void appliquer(Inscription i, InscriptionRequest r) {
        i.setAnneeScolaire(i.getClasse().getAnneeScolaire()); i.setDateInscription(r.dateInscription());
        i.setDateFin(r.dateFin()); i.setStatut(r.statut());
    }
}
