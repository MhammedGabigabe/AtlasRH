package ma.atlasrh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveRequestRejectionRequest(
        @NotBlank(message = "Le commentaire de refus est obligatoire.")
        @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères.")
        String comment
) {
}
