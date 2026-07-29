package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Enseignement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnseignementRepository extends JpaRepository<Enseignement, Long> {
    boolean existsByEnseignantIdAndMatiereIdAndClasseIdAndAnneeScolaire(
            Long enseignantId, Long matiereId, Long classeId, String anneeScolaire);
    boolean existsByEnseignantIdAndMatiereIdAndClasseIdAndAnneeScolaireAndIdNot(
            Long enseignantId, Long matiereId, Long classeId, String anneeScolaire, Long id);
    List<Enseignement> findByClasseIdOrderByMatiereNom(Long classeId);
    List<Enseignement> findByEnseignantIdOrderByAnneeScolaireDesc(Long enseignantId);
}
