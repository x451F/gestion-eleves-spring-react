package fr.afpa.gestioneleves.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageService {
    StoredPhoto stocker(MultipartFile fichier);
    LoadedPhoto charger(String nomStockage, String typeMime);
    void supprimer(String nomStockage);

    record StoredPhoto(String nomStockage, String typeMime, long taille) {
    }

    record LoadedPhoto(byte[] contenu, String typeMime) {
    }
}
