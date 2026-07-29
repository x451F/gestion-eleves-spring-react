package fr.afpa.gestioneleves.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "eleve")
public class Eleve extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_dossier", nullable = false, unique = true, length = 50)
    private String numeroDossier;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(name = "photo_nom_stockage")
    private String photoNomStockage;

    @Column(name = "photo_type_mime", length = 50)
    private String photoTypeMime;

    @Column(name = "photo_taille")
    private Long photoTaille;

    @Column(nullable = false)
    private boolean actif = true;

    public Long getId() { return id; }
    public String getNumeroDossier() { return numeroDossier; }
    public void setNumeroDossier(String numeroDossier) { this.numeroDossier = numeroDossier; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getPhotoNomStockage() { return photoNomStockage; }
    public void setPhotoNomStockage(String photoNomStockage) { this.photoNomStockage = photoNomStockage; }
    public String getPhotoTypeMime() { return photoTypeMime; }
    public void setPhotoTypeMime(String photoTypeMime) { this.photoTypeMime = photoTypeMime; }
    public Long getPhotoTaille() { return photoTaille; }
    public void setPhotoTaille(Long photoTaille) { this.photoTaille = photoTaille; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
