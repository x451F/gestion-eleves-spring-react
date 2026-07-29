package fr.afpa.gestioneleves.storage;

import fr.afpa.gestioneleves.config.StorageProperties;
import fr.afpa.gestioneleves.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class FileSystemPhotoStorageServiceTest {
    @TempDir Path temp;

    @Test void acceptePngEtGenereNomSur() {
        var service = service(1000);
        var photo = service.stocker(new MockMultipartFile("file", "avatar.png", "image/png", png()));
        assertThat(photo.nomStockage()).matches("[0-9a-f-]{36}\\.png");
        assertThat(temp.resolve(photo.nomStockage())).isRegularFile();
    }
    @Test void refuseFichierVide() {
        assertThatThrownBy(() -> service(1000).stocker(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(FileStorageException.class);
    }
    @Test void refuseTypeNonSupporte() {
        assertThatThrownBy(() -> service(1000).stocker(new MockMultipartFile("file", "x.txt", "text/plain", "abc".getBytes())))
                .isInstanceOf(UnsupportedPhotoTypeException.class);
    }
    @Test void refuseTypeAnnonceDifferentDuContenu() {
        assertThatThrownBy(() -> service(1000).stocker(new MockMultipartFile("file", "x.jpg", "image/jpeg", png())))
                .isInstanceOf(UnsupportedPhotoTypeException.class);
    }
    @Test void refuseFichierTropGrand() {
        assertThatThrownBy(() -> service(4).stocker(new MockMultipartFile("file", "x.png", "image/png", png())))
                .isInstanceOf(PhotoTooLargeException.class);
    }
    @Test void traverséeDeCheminImpossible() {
        assertThatThrownBy(() -> service(1000).charger("../../secret.png", "image/png"))
                .isInstanceOf(FileStorageException.class);
    }
    @Test void suppressionNeCibleQueNomGenere() {
        var service = service(1000);
        var photo = service.stocker(new MockMultipartFile("file", "avatar.png", "image/png", png()));
        service.supprimer(photo.nomStockage());
        assertThat(temp.resolve(photo.nomStockage())).doesNotExist();
    }
    private FileSystemPhotoStorageService service(long max) {
        return new FileSystemPhotoStorageService(new StorageProperties(temp, max));
    }
    private byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }
}
