package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DateFinRequest(@NotNull LocalDate dateFin) { }
