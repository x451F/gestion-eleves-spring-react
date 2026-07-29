package fr.afpa.gestioneleves.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bulletin_ligne",
        uniqueConstraints = @UniqueConstraint(name = "uk_bulletin_ligne_matiere",
                columnNames = {"bulletin_id", "code_matiere"}))
public class BulletinLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bulletin_id", nullable = false)
    private Bulletin bulletin;

    @Column(name = "code_matiere", nullable = false, length = 50)
    private String codeMatiere;

    @Column(name = "nom_matiere", nullable = false, length = 100)
    private String nomMatiere;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal moyenne;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal coefficient;

    @Column(name = "nombre_notes", nullable = false)
    private int nombreNotes;

    public Long getId() { return id; }
    public Bulletin getBulletin() { return bulletin; }
    public void setBulletin(Bulletin bulletin) { this.bulletin = bulletin; }
    public String getCodeMatiere() { return codeMatiere; }
    public void setCodeMatiere(String codeMatiere) { this.codeMatiere = codeMatiere; }
    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }
    public BigDecimal getMoyenne() { return moyenne; }
    public void setMoyenne(BigDecimal moyenne) { this.moyenne = moyenne; }
    public BigDecimal getCoefficient() { return coefficient; }
    public void setCoefficient(BigDecimal coefficient) { this.coefficient = coefficient; }
    public int getNombreNotes() { return nombreNotes; }
    public void setNombreNotes(int nombreNotes) { this.nombreNotes = nombreNotes; }
}
