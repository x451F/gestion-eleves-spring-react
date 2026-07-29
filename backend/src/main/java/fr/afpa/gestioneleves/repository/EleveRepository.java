package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleveRepository extends JpaRepository<Eleve, Long> {
    boolean existsByNumeroDossierIgnoreCase(String numeroDossier);
    boolean existsByNumeroDossierIgnoreCaseAndIdNot(String numeroDossier, Long id);
}
