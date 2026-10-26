package ma.atlasrh.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestCreateRequest(
        @NotNull(message = "Le type de congé est obligatoire.")
        Long leaveTypeId,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate startDate,

        @NotNull(message = "La date de fin est obligatoire.")
        LocalDate endDate,

        @Size(max = 255, message = "Le motif ne peut pas dépasser 255 caractères.")
        String reason
) {
}
