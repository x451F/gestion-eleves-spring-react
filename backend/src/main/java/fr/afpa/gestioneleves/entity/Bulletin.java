package fr.afpa.gestioneleves.entity;

import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import fr.afpa.gestioneleves.enumtype.StatutBulletin;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bulletin")
public class Bulletin extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodeBulletin periode;

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutBulletin statut;

    @Column(name = "moyenne_generale", nullable = false, precision = 5, scale = 2)
    private BigDecimal moyenneGenerale;

    @Column(length = 1000)
    private String appreciation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_precedente_id")
    private Bulletin versionPrecedente;

    @OneToMany(mappedBy = "bulletin", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nomMatiere ASC")
    private List<BulletinLigne> lignes = new ArrayList<>();

    @Version
    private long version;

    public void ajouterLigne(BulletinLigne ligne) {
        ligne.setBulletin(this);
        lignes.add(ligne);
    }

    public Long getId() { return id; }
    public Inscription getInscription() { return inscription; }
    public void setInscription(Inscription inscription) { this.inscription = inscription; }
    public PeriodeBulletin getPeriode() { return periode; }
    public void setPeriode(PeriodeBulletin periode) { this.periode = periode; }
    public LocalDateTime getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDateTime dateGeneration) { this.dateGeneration = dateGeneration; }
    public StatutBulletin getStatut() { return statut; }
    public void setStatut(StatutBulletin statut) { this.statut = statut; }
    public BigDecimal getMoyenneGenerale() { return moyenneGenerale; }
    public void setMoyenneGenerale(BigDecimal moyenneGenerale) { this.moyenneGenerale = moyenneGenerale; }
    public String getAppreciation() { return appreciation; }
    public void setAppreciation(String appreciation) { this.appreciation = appreciation; }
    public List<BulletinLigne> getLignes() { return lignes; }
    public long getVersion() { return version; }
    public Bulletin getVersionPrecedente() { return versionPrecedente; }
    public void setVersionPrecedente(Bulletin versionPrecedente) { this.versionPrecedente = versionPrecedente; }
}
