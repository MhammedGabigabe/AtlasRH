package ma.atlasrh.dto.response;

import ma.atlasrh.enums.ContractStatus;
import ma.atlasrh.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(
        Long id,
        ContractType type,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal grossSalary,
        ContractStatus status,
        Long employeeId
) {
}
