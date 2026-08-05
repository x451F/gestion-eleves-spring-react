package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token join fetch token.utilisateur where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update PasswordResetToken token set token.revokedAt = :now, token.revocationReason = :reason "
            + "where token.utilisateur.id = :userId and token.usedAt is null and token.revokedAt is null")
    int revokeUnusedForUser(@Param("userId") Long userId, @Param("now") java.time.LocalDateTime now,
                             @Param("reason") String reason);
}
