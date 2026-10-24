package ma.atlasrh.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ma.atlasrh.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(

        @NotNull(message = "Le type de contrat est obligatoire.")
        ContractType type,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Le salaire brut est obligatoire.")
        @Positive(message = "Le salaire brut doit être strictement positif.")
        BigDecimal grossSalary
) {
}
