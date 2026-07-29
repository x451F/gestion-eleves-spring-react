package fr.afpa.gestioneleves.entity;

import fr.afpa.gestioneleves.enumtype.Role;
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
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public Long getId() { return id; }
    public String getEmailNormalise() { return emailNormalise; }
    public void setEmailNormalise(String emailNormalise) { this.emailNormalise = emailNormalise; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
