package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Inscription;
import fr.afpa.gestioneleves.enumtype.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    boolean existsByEleveIdAndAnneeScolaireAndStatut(Long eleveId, String anneeScolaire, StatutInscription statut);
    boolean existsByEleveIdAndAnneeScolaireAndStatutAndIdNot(Long eleveId, String anneeScolaire, StatutInscription statut, Long id);
    List<Inscription> findByEleveIdOrderByAnneeScolaireDesc(Long eleveId);
    List<Inscription> findByClasseIdOrderByAnneeScolaireDesc(Long classeId);
    boolean existsByIdAndEleveId(Long id, Long eleveId);
    @Query("select (count(i) > 0) from Inscription i join Enseignement e on e.classe.id = i.classe.id and e.anneeScolaire = i.anneeScolaire "
            + "where i.eleve.id = :eleveId and e.enseignant.utilisateur.id = :utilisateurId")
    boolean existsVisibleToTeacher(@Param("eleveId") Long eleveId, @Param("utilisateurId") Long utilisateurId);
    @Query("select (count(i) > 0) from Inscription i join Enseignement e on e.classe.id = i.classe.id and e.anneeScolaire = i.anneeScolaire where i.id = :id and e.enseignant.utilisateur.id = :utilisateurId")
    boolean existsVisibleToTeacherById(@Param("id") Long id, @Param("utilisateurId") Long utilisateurId);
    @Query("select i from Inscription i join Enseignement e on e.classe.id = i.classe.id and e.anneeScolaire = i.anneeScolaire where i.eleve.id = :eleveId and e.enseignant.utilisateur.id = :utilisateurId order by i.anneeScolaire desc")
    List<Inscription> findVisibleToTeacherByEleveId(@Param("eleveId") Long eleveId, @Param("utilisateurId") Long utilisateurId);
}
