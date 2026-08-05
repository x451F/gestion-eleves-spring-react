package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Utilisateur;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmailNormalise(String emailNormalise);
    boolean existsByRole(fr.afpa.gestioneleves.enumtype.Role role);

    @Query(value = "select pg_advisory_xact_lock(814204)", nativeQuery = true)
    Object lockBootstrapAdminCreation();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select utilisateur from Utilisateur utilisateur where utilisateur.id = :id")
    Optional<Utilisateur> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select utilisateur from Utilisateur utilisateur where utilisateur.role = :role and utilisateur.statut = :statut")
    List<Utilisateur> findByRoleAndStatutForUpdate(@Param("role") Role role, @Param("statut") StatutUtilisateur statut);
}
