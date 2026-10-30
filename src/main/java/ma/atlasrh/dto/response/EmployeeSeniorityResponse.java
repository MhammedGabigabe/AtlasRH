package ma.atlasrh.dto.response;

import java.time.LocalDate;

public record EmployeeSeniorityResponse(
        LocalDate hireDate,
        int years,
        int months
) {
}
