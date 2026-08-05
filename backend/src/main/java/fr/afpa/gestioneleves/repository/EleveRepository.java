package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EleveRepository extends JpaRepository<Eleve, Long> {
    boolean existsByNumeroDossierIgnoreCase(String numeroDossier);
    boolean existsByNumeroDossierIgnoreCaseAndIdNot(String numeroDossier, Long id);
    @Query("select distinct i.eleve from Inscription i join Enseignement e on e.classe.id = i.classe.id and e.anneeScolaire = i.anneeScolaire where e.enseignant.utilisateur.id = :userId")
    List<Eleve> findVisibleToTeacher(@Param("userId") Long userId);
    @Query("select distinct l.eleve from EleveResponsable l where l.responsable.utilisateur.id = :userId and l.validFrom <= current_date and (l.validTo is null or l.validTo >= current_date)")
    List<Eleve> findVisibleToGuardian(@Param("userId") Long userId);
}
