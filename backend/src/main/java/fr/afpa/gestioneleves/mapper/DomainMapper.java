package fr.afpa.gestioneleves.mapper;

import fr.afpa.gestioneleves.dto.response.*;
import fr.afpa.gestioneleves.entity.*;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

    public EleveResponse toResponse(Eleve e) {
        return new EleveResponse(e.getId(), e.getNumeroDossier(), e.getNom(), e.getPrenom(),
                e.getDateNaissance(), e.getEmail(), e.getTelephone(), e.isActif(),
                e.getPhotoNomStockage() != null);
    }

    public ClasseResponse toResponse(Classe c) {
        return new ClasseResponse(c.getId(), c.getCode(), c.getNom(), c.getNiveau(),
                c.getAnneeScolaire(), c.isActif());
    }

    public InscriptionResponse toResponse(Inscription i) {
        return new InscriptionResponse(i.getId(), i.getEleve().getId(),
                i.getEleve().getNom() + " " + i.getEleve().getPrenom(),
                i.getClasse().getId(), i.getClasse().getNom(), i.getAnneeScolaire(),
                i.getDateInscription(), i.getDateFin(), i.getStatut());
    }

    public EnseignantResponse toResponse(Enseignant e) {
        return new EnseignantResponse(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(),
                e.getEmail(), e.getUtilisateur() == null ? null : e.getUtilisateur().getId(), e.isActif());
    }

    public MatiereResponse toResponse(Matiere m) {
        return new MatiereResponse(m.getId(), m.getCode(), m.getNom(), m.getCoefficientDefaut(), m.isActif());
    }

    public EnseignementResponse toResponse(Enseignement e) {
        return new EnseignementResponse(e.getId(), e.getEnseignant().getId(),
                e.getEnseignant().getNom() + " " + e.getEnseignant().getPrenom(),
                e.getMatiere().getId(), e.getMatiere().getNom(), e.getClasse().getId(),
                e.getClasse().getNom(), e.getAnneeScolaire(), e.getCoefficientMatiere());
    }

    public NoteResponse toResponse(Note n) {
        return new NoteResponse(n.getId(), n.getInscription().getId(), n.getEnseignement().getId(),
                n.getEnseignement().getMatiere().getNom(), n.getPeriode(), n.getValeur(),
                n.getBareme(), n.getCoefficient(), n.getDateEvaluation(), n.getLibelle(), n.getCommentaire());
    }

    public ResponsableResponse toResponse(Responsable r) {
        return new ResponsableResponse(r.getId(), r.getNom(), r.getPrenom(), r.getEmail(),
                r.getTelephone(), r.getUtilisateur() == null ? null : r.getUtilisateur().getId(), r.isActif());
    }

    public EleveResponsableResponse toResponse(EleveResponsable lien) {
        return new EleveResponsableResponse(lien.getId(), lien.getEleve().getId(),
                lien.getEleve().getNom() + " " + lien.getEleve().getPrenom(),
                lien.getResponsable().getId(),
                lien.getResponsable().getNom() + " " + lien.getResponsable().getPrenom(),
                lien.getLienParente(), lien.isResponsablePrincipal(), lien.isAutoriteParentale(),
                lien.isContactUrgence());
    }

    public BulletinResponse toResponse(Bulletin b) {
        var lignes = b.getLignes().stream()
                .map(l -> new BulletinResponse.Ligne(l.getCodeMatiere(), l.getNomMatiere(),
                        l.getMoyenne(), l.getCoefficient(), l.getNombreNotes()))
                .toList();
        return new BulletinResponse(b.getId(), b.getInscription().getId(),
                b.getInscription().getEleve().getNom() + " " + b.getInscription().getEleve().getPrenom(),
                b.getInscription().getClasse().getNom(), b.getInscription().getAnneeScolaire(),
                b.getPeriode(), b.getDateGeneration(), b.getStatut(), b.getMoyenneGenerale(),
                b.getAppreciation(), lignes);
    }

    public UtilisateurResponse toResponse(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getEmailNormalise(), u.getRole(),
                u.isActif(), u.getCreatedAt(), u.getLastLoginAt());
    }
}
