package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {
    boolean existsByMatriculeIgnoreCase(String matricule);
    boolean existsByMatriculeIgnoreCaseAndIdNot(String matricule, Long id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
