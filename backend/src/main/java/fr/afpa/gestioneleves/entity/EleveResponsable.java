package fr.afpa.gestioneleves.entity;

import fr.afpa.gestioneleves.enumtype.LienParente;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "eleve_responsable",
        uniqueConstraints = @UniqueConstraint(name = "uk_eleve_responsable", columnNames = {"eleve_id", "responsable_id"}))
public class EleveResponsable extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Responsable responsable;

    @Enumerated(EnumType.STRING)
    @Column(name = "lien_parente", nullable = false, length = 30)
    private LienParente lienParente;

    @Column(name = "responsable_principal", nullable = false)
    private boolean responsablePrincipal;

    @Column(name = "autorite_parentale", nullable = false)
    private boolean autoriteParentale = true;

    @Column(name = "contact_urgence", nullable = false)
    private boolean contactUrgence;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    public Long getId() { return id; }
    public Eleve getEleve() { return eleve; }
    public void setEleve(Eleve eleve) { this.eleve = eleve; }
    public Responsable getResponsable() { return responsable; }
    public void setResponsable(Responsable responsable) { this.responsable = responsable; }
    public LienParente getLienParente() { return lienParente; }
    public void setLienParente(LienParente lienParente) { this.lienParente = lienParente; }
    public boolean isResponsablePrincipal() { return responsablePrincipal; }
    public void setResponsablePrincipal(boolean responsablePrincipal) { this.responsablePrincipal = responsablePrincipal; }
    public boolean isAutoriteParentale() { return autoriteParentale; }
    public void setAutoriteParentale(boolean autoriteParentale) { this.autoriteParentale = autoriteParentale; }
    public boolean isContactUrgence() { return contactUrgence; }
    public void setContactUrgence(boolean contactUrgence) { this.contactUrgence = contactUrgence; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}
