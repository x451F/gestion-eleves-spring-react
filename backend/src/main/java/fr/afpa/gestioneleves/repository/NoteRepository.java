package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Note;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByInscriptionIdOrderByDateEvaluation(Long inscriptionId);
    List<Note> findByInscriptionEleveIdOrderByDateEvaluation(Long eleveId);
    List<Note> findByInscriptionIdAndPeriode(Long inscriptionId, PeriodeBulletin periode);
    List<Note> findByInscriptionIdAndPeriodeAndEnseignementEnseignantUtilisateurId(Long inscriptionId, PeriodeBulletin periode, Long utilisateurId);
    boolean existsByIdAndEnseignementEnseignantUtilisateurId(Long id, Long utilisateurId);
    List<Note> findByInscriptionEleveIdAndEnseignementEnseignantUtilisateurIdOrderByDateEvaluation(Long eleveId, Long utilisateurId);
    List<Note> findByInscriptionIdAndEnseignementEnseignantUtilisateurIdOrderByDateEvaluation(Long inscriptionId, Long utilisateurId);
}
