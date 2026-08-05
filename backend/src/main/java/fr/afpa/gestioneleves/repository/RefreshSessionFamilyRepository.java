package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.RefreshSessionFamily;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshSessionFamilyRepository extends JpaRepository<RefreshSessionFamily, Long> {
    List<RefreshSessionFamily> findByUtilisateurId(Long utilisateurId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select family from RefreshSessionFamily family where family.id = :id")
    Optional<RefreshSessionFamily> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select family from RefreshSessionFamily family where family.utilisateur.id = :userId")
    List<RefreshSessionFamily> findByUtilisateurIdForUpdate(@Param("userId") Long userId);
}
