package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.config.MailProperties;
import fr.afpa.gestioneleves.entity.SecurityEvent;
import fr.afpa.gestioneleves.entity.Utilisateur;
import fr.afpa.gestioneleves.enumtype.SecurityEventType;
import fr.afpa.gestioneleves.repository.SecurityEventRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AccountMailService {
    private final JavaMailSender mailSender;
    private final MailProperties properties;
    private final SecurityEventRepository eventRepository;
    private final Clock clock;

    public AccountMailService(JavaMailSender mailSender, MailProperties properties,
                              SecurityEventRepository eventRepository, Clock clock) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    public boolean sendActivation(Utilisateur user, String rawToken) {
        return deliver(user, "activation", "Activation de votre compte", "Bonjour,\n\n"
                + "Votre compte a été créé. Choisissez votre mot de passe en ouvrant ce lien :\n"
                + link(properties.activationUrl(), rawToken)
                + "\n\nCe lien est valable pendant 24 heures.\n\nL’équipe de gestion des élèves");
    }

    @Transactional
    public boolean sendPasswordReset(Utilisateur user, String rawToken) {
        return deliver(user, "password_reset", "Réinitialisation de votre mot de passe", "Bonjour,\n\n"
                + "Pour choisir un nouveau mot de passe, ouvrez ce lien :\n"
                + link(properties.resetUrl(), rawToken)
                + "\n\nCe lien est valable pendant 30 minutes. Si vous n’êtes pas à l’origine de cette demande, ignorez cet e-mail."
                + "\n\nL’équipe de gestion des élèves");
    }

    private boolean deliver(Utilisateur user, String kind, String subject, String text) {
        record(user, SecurityEventType.EMAIL_DELIVERY_ATTEMPT, "ATTEMPTED", kind);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.sender());
            message.setTo(user.getEmailNormalise());
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            record(user, SecurityEventType.EMAIL_DELIVERY_SUCCEEDED, "SUCCEEDED", kind);
            return true;
        } catch (MailException ex) {
            record(user, SecurityEventType.EMAIL_DELIVERY_FAILED, "FAILED", kind);
            return false;
        }
    }

    private String link(String baseUrl, String rawToken) {
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("token", rawToken).build().encode().toUriString();
    }

    private void record(Utilisateur user, SecurityEventType type, String deliveryStatus, String details) {
        SecurityEvent event = new SecurityEvent();
        event.setUtilisateur(user);
        event.setEventType(type);
        event.setOccurredAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        event.setDeliveryStatus(deliveryStatus);
        event.setDetails(details);
        eventRepository.save(event);
    }
}
