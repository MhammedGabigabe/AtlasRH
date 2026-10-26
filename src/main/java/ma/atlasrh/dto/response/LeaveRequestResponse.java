package ma.atlasrh.dto.response;

import ma.atlasrh.enums.LeaveRequestStatus;

import java.time.LocalDate;

public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        Long leaveTypeId,
        LocalDate startDate,
        LocalDate endDate,
        Integer numberOfDays,
        String reason,
        LeaveRequestStatus status
) {
}
