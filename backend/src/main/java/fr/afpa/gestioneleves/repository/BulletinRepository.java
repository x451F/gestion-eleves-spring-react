package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.Bulletin;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulletinRepository extends JpaRepository<Bulletin, Long> {
    boolean existsByInscriptionIdAndPeriode(Long inscriptionId, PeriodeBulletin periode);
    List<Bulletin> findByInscriptionIdOrderByDateGenerationDesc(Long inscriptionId);
}
