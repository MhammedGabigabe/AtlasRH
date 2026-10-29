package ma.atlasrh.dto.request;

import jakarta.validation.constraints.Size;

public record LeaveRequestDecisionRequest(
        @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères.")
        String managerComment
) {
}
