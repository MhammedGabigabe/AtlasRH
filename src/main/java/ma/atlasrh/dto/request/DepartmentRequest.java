package ma.atlasrh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(

        @NotBlank(message = "Le nom du département est obligatoire.")
        @Size(max = 100, message = "Le nom du département ne peut pas dépasser 100 caractères.")
        String name,

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères.")
        String description
) {
}
