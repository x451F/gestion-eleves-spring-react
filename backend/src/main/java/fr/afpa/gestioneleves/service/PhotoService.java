package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.response.PhotoMetadataResponse;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.repository.EleveRepository;
import fr.afpa.gestioneleves.storage.PhotoStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class PhotoService {
    private final EleveRepository eleveRepository;
    private final EleveService eleveService;
    private final PhotoStorageService storage;

    public PhotoService(EleveRepository eleveRepository, EleveService eleveService, PhotoStorageService storage) {
        this.eleveRepository = eleveRepository; this.eleveService = eleveService; this.storage = storage;
    }
    public PhotoMetadataResponse stocker(Long eleveId, MultipartFile fichier) {
        var eleve = eleveService.trouver(eleveId);
        var ancienne = eleve.getPhotoNomStockage();
        var photo = storage.stocker(fichier);
        eleve.setPhotoNomStockage(photo.nomStockage());
        eleve.setPhotoTypeMime(photo.typeMime());
        eleve.setPhotoTaille(photo.taille());
        eleveRepository.save(eleve);
        if (ancienne != null) storage.supprimer(ancienne);
        return new PhotoMetadataResponse(eleveId, photo.typeMime(), photo.taille(), "/api/eleves/" + eleveId + "/photo");
    }
    @Transactional(readOnly = true)
    public PhotoStorageService.LoadedPhoto charger(Long eleveId) {
        var eleve = eleveService.trouver(eleveId);
        if (eleve.getPhotoNomStockage() == null) throw new ResourceNotFoundException("Cet élève n'a pas de photo");
        return storage.charger(eleve.getPhotoNomStockage(), eleve.getPhotoTypeMime());
    }
    public void supprimer(Long eleveId) {
        var eleve = eleveService.trouver(eleveId);
        if (eleve.getPhotoNomStockage() == null) throw new ResourceNotFoundException("Cet élève n'a pas de photo");
        storage.supprimer(eleve.getPhotoNomStockage());
        eleve.setPhotoNomStockage(null); eleve.setPhotoTypeMime(null); eleve.setPhotoTaille(null);
        eleveRepository.save(eleve);
    }
}
