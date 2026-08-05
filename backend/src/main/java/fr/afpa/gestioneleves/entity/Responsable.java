package fr.afpa.gestioneleves.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "responsable")
public class Responsable extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Deprecated(forRemoval = false)
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "legacy_email", length = 180)
    private String legacyEmail;

    @Column(length = 30)
    private String telephone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private boolean actif = true;

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLegacyEmail() { return legacyEmail; }
    public void setLegacyEmail(String legacyEmail) { this.legacyEmail = legacyEmail; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
