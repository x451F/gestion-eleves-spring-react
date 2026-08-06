package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.entity.Bulletin;
import fr.afpa.gestioneleves.entity.Inscription;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutBulletin;
import fr.afpa.gestioneleves.exception.ResourceNotFoundException;
import fr.afpa.gestioneleves.repository.BulletinRepository;
import fr.afpa.gestioneleves.repository.EnseignantRepository;
import fr.afpa.gestioneleves.repository.EnseignementRepository;
import fr.afpa.gestioneleves.repository.EleveResponsableRepository;
import fr.afpa.gestioneleves.repository.InscriptionRepository;
import fr.afpa.gestioneleves.repository.NoteRepository;
import fr.afpa.gestioneleves.repository.ResponsableRepository;
import fr.afpa.gestioneleves.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database-backed authorization policy used by every business controller. */
@Service
@Transactional(readOnly = true)
public class AccessPolicyService {
    private final EnseignantRepository enseignants;
    private final ResponsableRepository responsables;
    private final EnseignementRepository enseignements;
    private final InscriptionRepository inscriptions;
    private final NoteRepository notes;
    private final BulletinRepository bulletins;
    private final EleveResponsableRepository liens;

    public AccessPolicyService(EnseignantRepository enseignants, ResponsableRepository responsables,
                               EnseignementRepository enseignements, InscriptionRepository inscriptions,
                               NoteRepository notes, BulletinRepository bulletins, EleveResponsableRepository liens) {
        this.enseignants = enseignants; this.responsables = responsables; this.enseignements = enseignements;
        this.inscriptions = inscriptions; this.notes = notes; this.bulletins = bulletins; this.liens = liens;
    }

    public void admin(AuthenticatedUser actor) { if (actor.role() != Role.ADMIN) forbidden(); }
    public void teacher(AuthenticatedUser actor) { if (actor.role() != Role.ENSEIGNANT) forbidden(); }
    public void teacherOrAdmin(AuthenticatedUser actor) { if (actor.role() != Role.ADMIN && actor.role() != Role.ENSEIGNANT) forbidden(); }
    public void guardianOrAdmin(AuthenticatedUser actor) { if (actor.role() != Role.ADMIN && actor.role() != Role.RESPONSABLE) forbidden(); }

    public void teacherProfile(AuthenticatedUser actor, Long teacherId) {
        if (actor.role() == Role.ADMIN) return;
        teacherOrAdmin(actor);
        if (!enseignants.findByUtilisateurId(actor.id()).map(e -> e.getId().equals(teacherId)).orElse(false)) hidden();
    }
    public void guardianProfile(AuthenticatedUser actor, Long guardianId) {
        if (actor.role() == Role.ADMIN) return;
        guardianOrAdmin(actor);
        if (!responsables.findByUtilisateurId(actor.id()).map(r -> r.getId().equals(guardianId)).orElse(false)) hidden();
    }
    public void teaching(AuthenticatedUser actor, Long teachingId) {
        if (actor.role() == Role.ADMIN) return;
        teacherOrAdmin(actor);
        if (!enseignements.existsByIdAndEnseignantUtilisateurId(teachingId, actor.id())) hidden();
    }
    public void classe(AuthenticatedUser actor, Long classeId) {
        if (actor.role() == Role.ADMIN) return;
        teacherOrAdmin(actor);
        if (!enseignements.existsByClasseIdAndEnseignantUtilisateurId(classeId, actor.id())) hidden();
    }
    public void eleve(AuthenticatedUser actor, Long eleveId) {
        if (actor.role() == Role.ADMIN) return;
        if (actor.role() == Role.ENSEIGNANT) {
            if (!inscriptions.existsVisibleToTeacher(eleveId, actor.id())) hidden();
            return;
        }
        if (actor.role() == Role.RESPONSABLE) {
            if (!liens.existsActiveByEleveIdAndResponsableUtilisateurId(eleveId, actor.id())) hidden();
            return;
        }
        forbidden();
    }
    public void inscription(AuthenticatedUser actor, Long inscriptionId) {
        if (actor.role() == Role.ADMIN) return;
        if (actor.role() == Role.ENSEIGNANT) {
            if (!inscriptions.existsVisibleToTeacherById(inscriptionId, actor.id())) hidden();
            return;
        }
        Inscription inscription = inscriptions.findById(inscriptionId).orElseThrow(this::hiddenException);
        eleve(actor, inscription.getEleve().getId());
    }
    public void note(AuthenticatedUser actor, Long noteId) {
        if (actor.role() == Role.ADMIN) return;
        if (actor.role() != Role.ENSEIGNANT) forbidden();
        if (!notes.existsByIdAndEnseignementEnseignantUtilisateurId(noteId, actor.id())) hidden();
    }
    public void bulletin(AuthenticatedUser actor, Long bulletinId) {
        if (actor.role() == Role.ADMIN) return;
        if (actor.role() == Role.ENSEIGNANT) {
            if (!bulletins.existsVisibleToTeacherById(bulletinId, actor.id())) hidden();
            return;
        }
        Bulletin bulletin = bulletins.findById(bulletinId).orElseThrow(this::hiddenException);
        if (actor.role() == Role.RESPONSABLE && bulletin.getStatut() != StatutBulletin.PUBLIE) hidden();
        eleve(actor, bulletin.getInscription().getEleve().getId());
    }
    public void writeNote(AuthenticatedUser actor, Long teachingId) { teaching(actor, teachingId); }
    private void forbidden() { throw new AccessDeniedException("Accès interdit."); }
    private void hidden() { throw hiddenException(); }
    private ResourceNotFoundException hiddenException() { return new ResourceNotFoundException("Ressource introuvable."); }
}
