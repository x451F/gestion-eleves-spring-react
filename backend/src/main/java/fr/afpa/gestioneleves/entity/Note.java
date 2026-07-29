package fr.afpa.gestioneleves.entity;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "note")
public class Note extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignement_id", nullable = false)
    private Enseignement enseignement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodeBulletin periode;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal valeur;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal bareme = new BigDecimal("20.00");

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal coefficient;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    @Column(length = 150)
    private String libelle;

    @Column(length = 500)
    private String commentaire;

    public Long getId() { return id; }
    public Inscription getInscription() { return inscription; }
    public void setInscription(Inscription inscription) { this.inscription = inscription; }
    public Enseignement getEnseignement() { return enseignement; }
    public void setEnseignement(Enseignement enseignement) { this.enseignement = enseignement; }
    public PeriodeBulletin getPeriode() { return periode; }
    public void setPeriode(PeriodeBulletin periode) { this.periode = periode; }
    public BigDecimal getValeur() { return valeur; }
    public void setValeur(BigDecimal valeur) { this.valeur = valeur; }
    public BigDecimal getBareme() { return bareme; }
    public void setBareme(BigDecimal bareme) { this.bareme = bareme; }
    public BigDecimal getCoefficient() { return coefficient; }
    public void setCoefficient(BigDecimal coefficient) { this.coefficient = coefficient; }
    public LocalDate getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(LocalDate dateEvaluation) { this.dateEvaluation = dateEvaluation; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}
