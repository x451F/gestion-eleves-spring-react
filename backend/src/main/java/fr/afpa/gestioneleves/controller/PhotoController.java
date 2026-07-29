package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.response.PhotoMetadataResponse;
import fr.afpa.gestioneleves.service.PhotoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/eleves/{eleveId}/photo")
public class PhotoController {
    private final PhotoService service;
    public PhotoController(PhotoService service) { this.service = service; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoMetadataResponse> stocker(@PathVariable Long eleveId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.stocker(eleveId, file));
    }
    @GetMapping
    public ResponseEntity<byte[]> charger(@PathVariable Long eleveId) {
        var photo = service.charger(eleveId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.typeMime()))
                .contentLength(photo.contenu().length).body(photo.contenu());
    }
    @DeleteMapping
    public ResponseEntity<Void> supprimer(@PathVariable Long eleveId) {
        service.supprimer(eleveId); return ResponseEntity.noContent().build();
    }
}
