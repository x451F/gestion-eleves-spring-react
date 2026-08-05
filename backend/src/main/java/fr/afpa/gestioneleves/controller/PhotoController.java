package fr.afpa.gestioneleves.controller;

import fr.afpa.gestioneleves.dto.response.PhotoMetadataResponse;
import fr.afpa.gestioneleves.service.PhotoService;
import fr.afpa.gestioneleves.service.AccessPolicyService;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/eleves/{eleveId}/photo")
public class PhotoController {
    private final PhotoService service;
    private final AccessPolicyService access;
    public PhotoController(PhotoService service, AccessPolicyService access) { this.service = service; this.access = access; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoMetadataResponse> stocker(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long eleveId, @RequestPart("file") MultipartFile file) {
        access.admin(actor);
        return ResponseEntity.ok(service.stocker(eleveId, file));
    }
    @GetMapping
    public ResponseEntity<byte[]> charger(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long eleveId) {
        access.eleve(actor, eleveId);
        var photo = service.charger(eleveId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.typeMime()))
                .contentLength(photo.contenu().length).body(photo.contenu());
    }
    @DeleteMapping
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long eleveId) {
        access.admin(actor);
        service.supprimer(eleveId); return ResponseEntity.noContent().build();
    }
}
