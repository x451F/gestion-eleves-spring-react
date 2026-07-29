package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.EleveResponsable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EleveResponsableRepository extends JpaRepository<EleveResponsable, Long> {
    boolean existsByEleveIdAndResponsableId(Long eleveId, Long responsableId);
    Optional<EleveResponsable> findByEleveIdAndResponsableId(Long eleveId, Long responsableId);
    List<EleveResponsable> findByResponsableId(Long responsableId);
    List<EleveResponsable> findByEleveId(Long eleveId);
}
