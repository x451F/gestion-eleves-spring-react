package fr.afpa.gestioneleves.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "enseignement",
        uniqueConstraints = @UniqueConstraint(name = "uk_enseignement_affectation",
                columnNames = {"enseignant_id", "matiere_id", "classe_id", "annee_scolaire"}))
public class Enseignement extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @Column(name = "annee_scolaire", nullable = false, length = 9)
    private String anneeScolaire;

    @Column(name = "coefficient_matiere", nullable = false, precision = 8, scale = 2)
    private BigDecimal coefficientMatiere;

    @Version
    private long version;

    public Long getId() { return id; }
    public Enseignant getEnseignant() { return enseignant; }
    public void setEnseignant(Enseignant enseignant) { this.enseignant = enseignant; }
    public Matiere getMatiere() { return matiere; }
    public void setMatiere(Matiere matiere) { this.matiere = matiere; }
    public Classe getClasse() { return classe; }
    public void setClasse(Classe classe) { this.classe = classe; }
    public String getAnneeScolaire() { return anneeScolaire; }
    public void setAnneeScolaire(String anneeScolaire) { this.anneeScolaire = anneeScolaire; }
    public BigDecimal getCoefficientMatiere() { return coefficientMatiere; }
    public void setCoefficientMatiere(BigDecimal coefficientMatiere) { this.coefficientMatiere = coefficientMatiere; }
    public long getVersion() { return version; }
}
