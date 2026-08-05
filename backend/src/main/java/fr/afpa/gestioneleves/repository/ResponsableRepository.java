package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Responsable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResponsableRepository extends JpaRepository<Responsable, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<Responsable> findByUtilisateurId(Long utilisateurId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select responsable from Responsable responsable where responsable.id = :id")
    Optional<Responsable> findByIdForUpdate(@Param("id") Long id);
}
