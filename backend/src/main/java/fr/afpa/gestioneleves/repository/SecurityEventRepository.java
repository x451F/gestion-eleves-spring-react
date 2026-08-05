package fr.afpa.gestioneleves.repository;

import fr.afpa.gestioneleves.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
