package fr.afpa.gestioneleves.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "matiere")
public class Matiere extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "coefficient_defaut", nullable = false, precision = 8, scale = 2)
    private BigDecimal coefficientDefaut;

    @Column(nullable = false)
    private boolean actif = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public BigDecimal getCoefficientDefaut() { return coefficientDefaut; }
    public void setCoefficientDefaut(BigDecimal coefficientDefaut) { this.coefficientDefaut = coefficientDefaut; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
