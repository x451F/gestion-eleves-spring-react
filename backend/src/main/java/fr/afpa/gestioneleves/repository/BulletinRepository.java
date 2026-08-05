package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Bulletin;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BulletinRepository extends JpaRepository<Bulletin, Long> {
    boolean existsByInscriptionIdAndPeriode(Long inscriptionId, PeriodeBulletin periode);
    List<Bulletin> findByInscriptionIdOrderByDateGenerationDesc(Long inscriptionId);
    @Query("select (count(b) > 0) from Bulletin b join Enseignement e on e.classe.id = b.inscription.classe.id and e.anneeScolaire = b.inscription.anneeScolaire where b.id = :id and e.enseignant.utilisateur.id = :utilisateurId")
    boolean existsVisibleToTeacherById(@Param("id") Long id, @Param("utilisateurId") Long utilisateurId);
    @Query("select b from Bulletin b join Enseignement e on e.classe.id = b.inscription.classe.id and e.anneeScolaire = b.inscription.anneeScolaire where b.inscription.id = :inscriptionId and e.enseignant.utilisateur.id = :utilisateurId order by b.dateGeneration desc")
    List<Bulletin> findVisibleToTeacherByInscriptionId(@Param("inscriptionId") Long inscriptionId, @Param("utilisateurId") Long utilisateurId);
}
