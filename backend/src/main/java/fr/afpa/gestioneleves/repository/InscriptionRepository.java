package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Inscription;
import fr.afpa.gestioneleves.enumtype.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    boolean existsByEleveIdAndAnneeScolaireAndStatut(Long eleveId, String anneeScolaire, StatutInscription statut);
    boolean existsByEleveIdAndAnneeScolaireAndStatutAndIdNot(Long eleveId, String anneeScolaire, StatutInscription statut, Long id);
    List<Inscription> findByEleveIdOrderByAnneeScolaireDesc(Long eleveId);
    List<Inscription> findByClasseIdOrderByAnneeScolaireDesc(Long classeId);
}
