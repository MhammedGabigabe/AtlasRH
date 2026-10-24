package ma.atlasrh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(

        @NotBlank(message = "Le matricule est obligatoire.")
        @Size(max = 20, message = "Le matricule ne peut pas dépasser 20 caractères.")
        String matricule,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
        String lastName,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
        String firstName,

        @NotBlank(message = "L'e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail n'est pas valide.")
        @Size(max = 150, message = "L'e-mail ne peut pas dépasser 150 caractères.")
        String email,

        @NotBlank(message = "Le poste est obligatoire.")
        @Size(max = 100, message = "Le poste ne peut pas dépasser 100 caractères.")
        String position,

        @NotNull(message = "La date d'embauche est obligatoire.")
        LocalDate hireDate,

        @NotNull(message = "Le département est obligatoire.")
        Long departmentId,

        Long managerId
) {
}
