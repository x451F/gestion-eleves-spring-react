package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.NoteRequest;
import fr.afpa.gestioneleves.dto.response.MoyennesResponse;
import fr.afpa.gestioneleves.dto.response.NoteResponse;
import fr.afpa.gestioneleves.entity.Note;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class NoteService {
    private static final BigDecimal VINGT = new BigDecimal("20.00");
    private final NoteRepository repository;
    private final InscriptionService inscriptionService;
    private final EnseignementService enseignementService;
    private final CalculMoyenneService calculMoyenneService;
    private final DomainMapper mapper;

    public NoteService(NoteRepository repository, InscriptionService inscriptionService,
                       EnseignementService enseignementService, CalculMoyenneService calculMoyenneService,
                       DomainMapper mapper) {
        this.repository = repository; this.inscriptionService = inscriptionService;
        this.enseignementService = enseignementService; this.calculMoyenneService = calculMoyenneService; this.mapper = mapper;
    }
    public NoteResponse creer(NoteRequest r) {
        verifierValeurs(r);
        var inscription = inscriptionService.trouver(r.inscriptionId());
        var enseignement = enseignementService.trouver(r.enseignementId());
        verifierCompatibilite(inscription.getClasse().getId(), inscription.getAnneeScolaire(),
                enseignement.getClasse().getId(), enseignement.getAnneeScolaire());
        Note note = new Note(); note.setInscription(inscription); note.setEnseignement(enseignement); appliquer(note, r);
        return mapper.toResponse(repository.save(note));
    }
    @Transactional(readOnly = true)
    public List<NoteResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public NoteResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    @Transactional(readOnly = true)
    public List<NoteResponse> parInscription(Long id) {
        inscriptionService.trouver(id); return repository.findByInscriptionIdOrderByDateEvaluation(id).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<NoteResponse> parEleve(Long id) {
        return repository.findByInscriptionEleveIdOrderByDateEvaluation(id).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public MoyennesResponse moyennes(Long inscriptionId, PeriodeBulletin periode) {
        inscriptionService.trouver(inscriptionId);
        return calculMoyenneService.calculer(inscriptionId, periode, repository.findByInscriptionIdAndPeriode(inscriptionId, periode));
    }
    public NoteResponse modifier(Long id, NoteRequest r) {
        verifierValeurs(r);
        Note note = trouver(id);
        var inscription = inscriptionService.trouver(r.inscriptionId());
        var enseignement = enseignementService.trouver(r.enseignementId());
        verifierCompatibilite(inscription.getClasse().getId(), inscription.getAnneeScolaire(),
                enseignement.getClasse().getId(), enseignement.getAnneeScolaire());
        note.setInscription(inscription); note.setEnseignement(enseignement); appliquer(note, r);
        return mapper.toResponse(repository.save(note));
    }
    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }
    public Note trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Note introuvable : " + id));
    }
    private void verifierValeurs(NoteRequest r) {
        if (r.valeur() == null || r.valeur().compareTo(BigDecimal.ZERO) < 0 || r.valeur().compareTo(VINGT) > 0)
            throw new BusinessRuleException("La note doit être comprise entre 0 et 20");
        if (r.coefficient() == null || r.coefficient().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("Le coefficient de note doit être strictement positif");
        if (r.bareme() == null || r.bareme().compareTo(VINGT) != 0)
            throw new BusinessRuleException("Le barème accepté est 20");
    }
    private void verifierCompatibilite(Long classeInscription, String anneeInscription, Long classeEnseignement, String anneeEnseignement) {
        if (!classeInscription.equals(classeEnseignement))
            throw new BusinessRuleException("La classe de l'inscription ne correspond pas à l'enseignement");
        if (!anneeInscription.equals(anneeEnseignement))
            throw new BusinessRuleException("L'année scolaire de l'inscription ne correspond pas à l'enseignement");
    }
    private void appliquer(Note n, NoteRequest r) {
        n.setPeriode(r.periode()); n.setValeur(r.valeur()); n.setBareme(r.bareme());
        n.setCoefficient(r.coefficient()); n.setDateEvaluation(r.dateEvaluation());
        n.setLibelle(r.libelle()); n.setCommentaire(r.commentaire());
    }
}
