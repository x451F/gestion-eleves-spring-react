package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Enseignant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {
    boolean existsByMatriculeIgnoreCase(String matricule);
    boolean existsByMatriculeIgnoreCaseAndIdNot(String matricule, Long id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<Enseignant> findByUtilisateurId(Long utilisateurId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select enseignant from Enseignant enseignant where enseignant.id = :id")
    Optional<Enseignant> findByIdForUpdate(@Param("id") Long id);
}
