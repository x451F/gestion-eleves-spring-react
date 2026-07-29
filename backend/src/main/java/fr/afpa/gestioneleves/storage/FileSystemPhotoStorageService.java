package fr.afpa.gestioneleves.storage;

import fr.afpa.gestioneleves.config.StorageProperties;
import fr.afpa.gestioneleves.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileSystemPhotoStorageService implements PhotoStorageService {
    private static final Pattern NOM_GENERE = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.(jpg|png|webp)$");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final Path racine;
    private final long tailleMax;

    public FileSystemPhotoStorageService(StorageProperties properties) {
        this.racine = properties.photoRoot().toAbsolutePath().normalize();
        this.tailleMax = properties.maxPhotoSize();
        try {
            Files.createDirectories(racine);
        } catch (IOException ex) {
            throw new FileStorageException("Impossible d'initialiser le stockage des photos", ex);
        }
    }

    @Override
    public StoredPhoto stocker(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) throw new FileStorageException("La photo est vide");
        if (fichier.getSize() > tailleMax) throw new PhotoTooLargeException("La photo dépasse la taille maximale");
        try {
            byte[] contenu = fichier.getBytes();
            String typeDetecte = detecterType(contenu);
            String typeAnnonce = fichier.getContentType() == null ? "" : fichier.getContentType().toLowerCase(Locale.ROOT);
            if (!typeDetecte.equals(typeAnnonce)) {
                throw new UnsupportedPhotoTypeException("Le contenu de la photo ne correspond pas à son type MIME");
            }
            String nom = UUID.randomUUID() + "." + EXTENSIONS.get(typeDetecte);
            Path destination = resoudre(nom);
            Files.write(destination, contenu, StandardOpenOption.CREATE_NEW);
            return new StoredPhoto(nom, typeDetecte, contenu.length);
        } catch (UnsupportedPhotoTypeException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileStorageException("Impossible de stocker la photo", ex);
        }
    }

    @Override
    public LoadedPhoto charger(String nomStockage, String typeMime) {
        validerNomGenere(nomStockage);
        Path fichier = resoudre(nomStockage);
        if (!Files.isRegularFile(fichier)) throw new ResourceNotFoundException("Photo introuvable");
        try {
            return new LoadedPhoto(Files.readAllBytes(fichier), typeMime);
        } catch (IOException ex) {
            throw new FileStorageException("Impossible de lire la photo", ex);
        }
    }

    @Override
    public void supprimer(String nomStockage) {
        if (nomStockage == null) return;
        validerNomGenere(nomStockage);
        try {
            Files.deleteIfExists(resoudre(nomStockage));
        } catch (IOException ex) {
            throw new FileStorageException("Impossible de supprimer la photo", ex);
        }
    }

    private Path resoudre(String nom) {
        Path chemin = racine.resolve(nom).normalize();
        if (!chemin.startsWith(racine)) throw new FileStorageException("Chemin de stockage invalide");
        return chemin;
    }

    private void validerNomGenere(String nom) {
        if (nom == null || !NOM_GENERE.matcher(nom).matches()) {
            throw new FileStorageException("Nom de photo stockée invalide");
        }
    }

    private String detecterType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff)
            return "image/jpeg";
        if (b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47
                && b[4] == 0x0d && b[5] == 0x0a && b[6] == 0x1a && b[7] == 0x0a)
            return "image/png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')
            return "image/webp";
        throw new UnsupportedPhotoTypeException("Seules les photos JPEG, PNG et WebP sont acceptées");
    }
}
