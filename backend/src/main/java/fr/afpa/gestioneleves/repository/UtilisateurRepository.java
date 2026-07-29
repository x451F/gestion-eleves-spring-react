package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmailNormalise(String emailNormalise);
}
