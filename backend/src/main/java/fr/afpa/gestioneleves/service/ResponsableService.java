package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.AssociationResponsableRequest;
import fr.afpa.gestioneleves.dto.request.ResponsableRequest;
import fr.afpa.gestioneleves.dto.response.EleveResponsableResponse;
import fr.afpa.gestioneleves.dto.response.ResponsableResponse;
import fr.afpa.gestioneleves.entity.EleveResponsable;
import fr.afpa.gestioneleves.entity.Responsable;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.exception.BusinessRuleException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.EleveResponsableRepository;
import fr.afpa.gestioneleves.repository.ResponsableRepository;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ResponsableService {
    private final ResponsableRepository repository;
    private final EleveResponsableRepository lienRepository;
    private final EleveService eleveService;
    private final DomainMapper mapper;

    public ResponsableService(ResponsableRepository repository, EleveResponsableRepository lienRepository,
                              EleveService eleveService, DomainMapper mapper) {
        this.repository = repository; this.lienRepository = lienRepository; this.eleveService = eleveService; this.mapper = mapper;
    }
    public ResponsableResponse creer(ResponsableRequest r) {
        if (repository.existsByEmailIgnoreCase(r.email())) throw new DuplicateResourceException("Cet email responsable est déjà utilisé");
        Responsable responsable = new Responsable(); appliquer(responsable, r);
        return mapper.toResponse(repository.save(responsable));
    }
    @Transactional(readOnly = true)
    public List<ResponsableResponse> lister() { return repository.findAll().stream().map(mapper::toResponse).toList(); }
    @Transactional(readOnly = true)
    public ResponsableResponse obtenir(Long id) { return mapper.toResponse(trouver(id)); }
    public ResponsableResponse modifier(Long id, ResponsableRequest r) {
        if (repository.existsByEmailIgnoreCaseAndIdNot(r.email(), id)) throw new DuplicateResourceException("Cet email responsable est déjà utilisé");
        Responsable responsable = trouver(id); appliquer(responsable, r);
        return mapper.toResponse(repository.save(responsable));
    }
    public void supprimer(Long id) { repository.delete(trouver(id)); repository.flush(); }
    public EleveResponsableResponse associer(Long responsableId, Long eleveId, AssociationResponsableRequest r) {
        if (lienRepository.existsByEleveIdAndResponsableId(eleveId, responsableId))
            throw new DuplicateResourceException("Ce responsable est déjà associé à cet élève");
        EleveResponsable lien = new EleveResponsable();
        lien.setResponsable(trouver(responsableId)); lien.setEleve(eleveService.trouver(eleveId));
        lien.setLienParente(r.lienParente()); lien.setResponsablePrincipal(r.responsablePrincipal());
        lien.setAutoriteParentale(r.autoriteParentale()); lien.setContactUrgence(r.contactUrgence());
        lien.setValidFrom(LocalDate.now());
        if (r.responsablePrincipal()) definirPrincipalVerrouille(eleveId, responsableId, lien);
        return mapper.toResponse(lienRepository.save(lien));
    }
    public void dissocier(Long responsableId, Long eleveId) {
        eleveService.verrouiller(eleveId);
        EleveResponsable lien = lienRepository.findByEleveIdAndResponsableId(eleveId, responsableId)
                .orElseThrow(() -> new ResourceNotFoundException("Association responsable-élève introuvable"));
        if (lien.getValidTo() != null && !lien.getValidTo().isAfter(LocalDate.now())) return;
        lien.setValidTo(LocalDate.now());
        lien.setResponsablePrincipal(false);
        lienRepository.save(lien);
    }
    public EleveResponsableResponse terminerLien(Long responsableId, Long eleveId, LocalDate dateFin) {
        eleveService.verrouiller(eleveId);
        EleveResponsable lien = lienRepository.findByEleveIdAndResponsableId(eleveId, responsableId)
                .orElseThrow(() -> new ResourceNotFoundException("Association responsable-élève introuvable"));
        if (dateFin.isBefore(lien.getValidFrom())) throw new BusinessRuleException("La date de fin ne peut pas précéder le début de la relation");
        if (lien.getValidTo() != null && !lien.getValidTo().isAfter(dateFin)) return mapper.toResponse(lien);
        lien.setValidTo(dateFin); lien.setResponsablePrincipal(false);
        return mapper.toResponse(lienRepository.save(lien));
    }
    public EleveResponsableResponse definirPrincipal(Long responsableId, Long eleveId) {
        eleveService.verrouiller(eleveId);
        EleveResponsable cible = lienRepository.findByEleveIdAndResponsableId(eleveId, responsableId)
                .orElseThrow(() -> new ResourceNotFoundException("Association responsable-élève introuvable"));
        if (!actif(cible)) throw new BusinessRuleException("Le responsable doit avoir une relation active avec l'élève");
        definirPrincipalVerrouille(eleveId, responsableId, cible);
        return mapper.toResponse(lienRepository.save(cible));
    }
    @Transactional(readOnly = true)
    public List<EleveResponsableResponse> eleves(Long responsableId) {
        trouver(responsableId); return lienRepository.findByResponsableId(responsableId).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<EleveResponsableResponse> elevesActifs(AuthenticatedUser actor) {
        return lienRepository.findActiveByResponsableUtilisateurId(actor.id()).stream().map(mapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<EleveResponsableResponse> responsables(Long eleveId) {
        eleveService.trouver(eleveId); return lienRepository.findByEleveId(eleveId).stream().map(mapper::toResponse).toList();
    }
    public Responsable trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable : " + id));
    }
    private void definirPrincipalVerrouille(Long eleveId, Long responsableId, EleveResponsable cible) {
        for (EleveResponsable lien : lienRepository.findByEleveIdForUpdate(eleveId)) {
            if (actif(lien) && lien.isResponsablePrincipal() && !lien.getResponsable().getId().equals(responsableId)) {
                lien.setResponsablePrincipal(false);
            }
        }
        lienRepository.flush();
        cible.setResponsablePrincipal(true);
    }
    private boolean actif(EleveResponsable lien) {
        LocalDate now = LocalDate.now();
        return !lien.getValidFrom().isAfter(now) && (lien.getValidTo() == null || lien.getValidTo().isAfter(now));
    }
    private void appliquer(Responsable responsable, ResponsableRequest r) {
        responsable.setNom(r.nom().trim()); responsable.setPrenom(r.prenom().trim());
        responsable.setEmail(r.email().trim().toLowerCase());
        responsable.setTelephone(r.telephone() == null || r.telephone().isBlank() ? null : r.telephone().trim());
        responsable.setActif(r.actif() == null || r.actif());
    }
}
