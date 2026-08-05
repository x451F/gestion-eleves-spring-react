package fr.afpa.gestioneleves.entity;

import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_event")
public class SecurityEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "refresh_family_id") private RefreshSessionFamily refreshFamily;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 60) private SecurityEventType eventType;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "delivery_status", length = 30) private String deliveryStatus;
    @Column(length = 1000) private String details;
    public Long getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public RefreshSessionFamily getRefreshFamily() { return refreshFamily; }
    public void setRefreshFamily(RefreshSessionFamily refreshFamily) { this.refreshFamily = refreshFamily; }
    public SecurityEventType getEventType() { return eventType; }
    public void setEventType(SecurityEventType eventType) { this.eventType = eventType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
