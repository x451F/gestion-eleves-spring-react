package fr.afpa.gestioneleves.entity;

import fr.afpa.gestioneleves.enumtype.Role;
import fr.afpa.gestioneleves.enumtype.StatutUtilisateur;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
public class Utilisateur extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_normalise", nullable = false, unique = true, length = 180)
    private String emailNormalise;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutUtilisateur statut = StatutUtilisateur.EN_ATTENTE_ACTIVATION;

    @Column(name = "email_verifie_at")
    private LocalDateTime emailVerifieAt;

    @Column(name = "token_version", nullable = false)
    @PositiveOrZero
    private long tokenVersion;

    @Version
    private long version;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public Long getId() { return id; }
    public String getEmailNormalise() { return emailNormalise; }
    public void setEmailNormalise(String emailNormalise) { this.emailNormalise = emailNormalise; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public StatutUtilisateur getStatut() { return statut; }
    public void setStatut(StatutUtilisateur statut) { this.statut = statut; }
    public boolean isActif() { return statut == StatutUtilisateur.ACTIF; }
    public LocalDateTime getEmailVerifieAt() { return emailVerifieAt; }
    public void setEmailVerifieAt(LocalDateTime emailVerifieAt) { this.emailVerifieAt = emailVerifieAt; }
    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
    public long getVersion() { return version; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
