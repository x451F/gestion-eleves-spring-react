package fr.afpa.gestioneleves.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_session")
public class RefreshSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "family_id", nullable = false, updatable = false) private RefreshSessionFamily family;
    @Column(nullable = false, updatable = false) private long generation;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @Column(name = "revoked_at") private LocalDateTime revokedAt;
    @Column(name = "revocation_reason", length = 100) private String revocationReason;
    @Column(name = "replaced_by_id") private Long replacedById;
    @Column(name = "replaced_by_generation") private Long replacedByGeneration;
    public Long getId() { return id; }
    public RefreshSessionFamily getFamily() { return family; }
    public void setFamily(RefreshSessionFamily family) { this.family = family; }
    public long getGeneration() { return generation; }
    public void setGeneration(long generation) { this.generation = generation; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }
    public Long getReplacedById() { return replacedById; }
    public Long getReplacedByGeneration() { return replacedByGeneration; }
    public void assignReplacement(long replacementId, long replacementGeneration) {
        if (replacedById != null && (!replacedById.equals(replacementId) || !replacedByGeneration.equals(replacementGeneration))) {
            throw new IllegalStateException("Refresh session replacement is immutable once assigned");
        }
        this.replacedById = replacementId;
        this.replacedByGeneration = replacementGeneration;
    }
}
