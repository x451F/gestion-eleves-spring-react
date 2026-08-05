package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.RefreshSessionFamily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefreshSessionFamilyRepository extends JpaRepository<RefreshSessionFamily, Long> {
    List<RefreshSessionFamily> findByUtilisateurId(Long utilisateurId);
}
