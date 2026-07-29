package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClasseRepository extends JpaRepository<Classe, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByNomIgnoreCaseAndAnneeScolaire(String nom, String anneeScolaire);
    boolean existsByNomIgnoreCaseAndAnneeScolaireAndIdNot(String nom, String anneeScolaire, Long id);
}
