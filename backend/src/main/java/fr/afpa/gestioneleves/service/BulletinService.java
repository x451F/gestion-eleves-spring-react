package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.BulletinGenerateRequest;
import fr.afpa.gestioneleves.dto.response.BulletinResponse;
import fr.afpa.gestioneleves.entity.Bulletin;
import fr.afpa.gestioneleves.entity.BulletinLigne;
import fr.afpa.gestioneleves.enumtype.StatutBulletin;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.BulletinRepository;
import fr.afpa.gestioneleves.repository.NoteRepository;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BulletinService {
    private final BulletinRepository repository;
    private final NoteRepository noteRepository;
    private final InscriptionService inscriptionService;
    private final CalculMoyenneService calculMoyenneService;
    private final DomainMapper mapper;

    public BulletinService(BulletinRepository repository, NoteRepository noteRepository,
                           InscriptionService inscriptionService, CalculMoyenneService calculMoyenneService,
                           DomainMapper mapper) {
        this.repository = repository; this.noteRepository = noteRepository;
        this.inscriptionService = inscriptionService; this.calculMoyenneService = calculMoyenneService; this.mapper = mapper;
    }
    public BulletinResponse generer(BulletinGenerateRequest r) {
        if (repository.existsByInscriptionIdAndPeriode(r.inscriptionId(), r.periode())) {
            var brouillon = repository.findByInscriptionIdAndPeriodeAndStatut(r.inscriptionId(), r.periode(), StatutBulletin.BROUILLON);
            if (brouillon.isEmpty()) throw new DuplicateResourceException("Un bulletin existe déjà pour cette inscription et cette période");
            repository.delete(brouillon.get()); repository.flush();
        }
        return genererSnapshot(r);
    }
    private BulletinResponse genererSnapshot(BulletinGenerateRequest r) {
        var inscription = inscriptionService.trouver(r.inscriptionId());
        var notes = noteRepository.findByInscriptionIdAndPeriode(r.inscriptionId(), r.periode());
        var moyennes = calculMoyenneService.calculer(r.inscriptionId(), r.periode(), notes);
        if (moyennes.matieres().isEmpty())
            throw new BusinessRuleException("Impossible de générer un bulletin sans note");
        Bulletin bulletin = new Bulletin();
        bulletin.setInscription(inscription); bulletin.setPeriode(r.periode());
        bulletin.setDateGeneration(LocalDateTime.now()); bulletin.setStatut(StatutBulletin.BROUILLON);
        bulletin.setMoyenneGenerale(moyennes.moyenneGenerale()); bulletin.setAppreciation(r.appreciation());
        moyennes.matieres().forEach(m -> {
            BulletinLigne ligne = new BulletinLigne();
            var note = notes.stream().filter(n -> n.getEnseignement().getMatiere().getId().equals(m.matiereId())).findFirst().orElseThrow();
            ligne.setCodeMatiere(note.getEnseignement().getMatiere().getCode());
            ligne.setNomMatiere(m.nom()); ligne.setMoyenne(m.moyenne());
            ligne.setCoefficient(m.coefficient()); ligne.setNombreNotes(m.nombreNotes());
            bulletin.ajouterLigne(ligne);
        });
        return mapper.toResponse(repository.save(bulletin));
    }
    @Transactional(readOnly = true)
    public List<BulletinResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public BulletinResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    @Transactional(readOnly = true)
    public List<BulletinResponse> parInscription(Long id) {
        inscriptionService.trouver(id);
        return repository.findByInscriptionIdOrderByDateGenerationDesc(id).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<BulletinResponse> parInscriptionPourEnseignant(Long id, AuthenticatedUser actor) {
        return repository.findVisibleToTeacherByInscriptionId(id, actor.id()).stream().map(mapper::toResponse).toList();
    }
    public BulletinResponse publier(Long id) {
        Bulletin bulletin = trouver(id);
        if (bulletin.getStatut() != StatutBulletin.BROUILLON) throw new BusinessRuleException("Seul un bulletin brouillon peut être publié");
        bulletin.setStatut(StatutBulletin.PUBLIE);
        return mapper.toResponse(repository.save(bulletin));
    }
    public BulletinResponse corriger(Long id, BulletinGenerateRequest request) {
        Bulletin precedent = trouver(id);
        if (precedent.getStatut() != StatutBulletin.PUBLIE) throw new BusinessRuleException("Seul un bulletin publié peut être corrigé");
        precedent.setStatut(StatutBulletin.REMPLACE); repository.saveAndFlush(precedent);
        BulletinResponse draft = genererSnapshot(request);
        Bulletin nouveau = trouver(draft.id()); nouveau.setVersionPrecedente(precedent);
        return mapper.toResponse(repository.save(nouveau));
    }
    public void supprimer(Long id) {
        Bulletin bulletin = trouver(id);
        if (bulletin.getStatut() != StatutBulletin.BROUILLON)
            throw new BusinessRuleException("Un bulletin publié ne peut pas être supprimé");
        repository.delete(bulletin);
    }
    public Bulletin trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bulletin introuvable : " + id));
    }
}
