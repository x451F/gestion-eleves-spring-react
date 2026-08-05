package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.EleveResponsable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface EleveResponsableRepository extends JpaRepository<EleveResponsable, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from EleveResponsable l where l.eleve.id = :eleveId")
    List<EleveResponsable> findByEleveIdForUpdate(@Param("eleveId") Long eleveId);
    boolean existsByEleveIdAndResponsableId(Long eleveId, Long responsableId);
    Optional<EleveResponsable> findByEleveIdAndResponsableId(Long eleveId, Long responsableId);
    List<EleveResponsable> findByResponsableId(Long responsableId);
    List<EleveResponsable> findByEleveId(Long eleveId);

    @Query("select (count(l) > 0) from EleveResponsable l where l.eleve.id = :eleveId "
            + "and l.responsable.utilisateur.id = :utilisateurId and l.validFrom <= current_date "
            + "and (l.validTo is null or l.validTo > current_date)")
    boolean existsActiveByEleveIdAndResponsableUtilisateurId(@Param("eleveId") Long eleveId,
                                                             @Param("utilisateurId") Long utilisateurId);
    @Query("select l from EleveResponsable l where l.responsable.utilisateur.id = :utilisateurId and l.validFrom <= current_date and (l.validTo is null or l.validTo > current_date)")
    List<EleveResponsable> findActiveByResponsableUtilisateurId(@Param("utilisateurId") Long utilisateurId);
}
