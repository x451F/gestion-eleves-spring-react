package fr.afpa.gestioneleves.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TransferInscriptionRequest(@NotNull Long classeId, @NotNull LocalDate dateTransfert) { }
