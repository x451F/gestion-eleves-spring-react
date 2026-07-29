package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.request.EleveRequest;
import fr.afpa.gestioneleves.dto.response.EleveResponse;
import fr.afpa.gestioneleves.entity.Eleve;
import fr.afpa.gestioneleves.exception.DuplicateResourceException;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.mapper.DomainMapper;
import fr.afpa.gestioneleves.repository.EleveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EleveService {

    private final EleveRepository repository;
    private final DomainMapper mapper;

    public EleveService(EleveRepository repository, DomainMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public EleveResponse creer(EleveRequest request) {
        if (repository.existsByNumeroDossierIgnoreCase(request.numeroDossier().trim())) {
            throw new DuplicateResourceException("Un élève possède déjà ce numéro de dossier");
        }
        Eleve eleve = new Eleve();
        appliquer(eleve, request);
        return mapper.toResponse(repository.save(eleve));
    }

    @Transactional(readOnly = true)
    public List<EleveResponse> lister() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EleveResponse obtenir(Long id) {
        return mapper.toResponse(trouver(id));
    }

    public EleveResponse modifier(Long id, EleveRequest request) {
        if (repository.existsByNumeroDossierIgnoreCaseAndIdNot(request.numeroDossier().trim(), id)) {
            throw new DuplicateResourceException("Un élève possède déjà ce numéro de dossier");
        }
        Eleve eleve = trouver(id);
        appliquer(eleve, request);
        return mapper.toResponse(repository.save(eleve));
    }

    public void supprimer(Long id) {
        repository.delete(trouver(id));
        repository.flush();
    }

    public Eleve trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable : " + id));
    }

    private void appliquer(Eleve eleve, EleveRequest request) {
        eleve.setNumeroDossier(request.numeroDossier().trim().toUpperCase());
        eleve.setNom(request.nom().trim());
        eleve.setPrenom(request.prenom().trim());
        eleve.setDateNaissance(request.dateNaissance());
        eleve.setEmail(normaliserOptionnel(request.email()));
        eleve.setTelephone(normaliserOptionnel(request.telephone()));
        eleve.setActif(request.actif() == null || request.actif());
    }

    private String normaliserOptionnel(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
