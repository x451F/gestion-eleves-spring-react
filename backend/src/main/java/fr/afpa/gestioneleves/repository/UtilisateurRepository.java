package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Utilisateur;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmailNormalise(String emailNormalise);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select utilisateur from Utilisateur utilisateur where utilisateur.id = :id")
    Optional<Utilisateur> findByIdForUpdate(@Param("id") Long id);
}
