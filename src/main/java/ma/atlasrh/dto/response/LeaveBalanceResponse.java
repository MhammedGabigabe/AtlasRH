package ma.atlasrh.dto.response;

public record LeaveBalanceResponse(
        String leaveType,
        Integer annualDays,
        long usedDays,
        long remainingDays
) {
}
