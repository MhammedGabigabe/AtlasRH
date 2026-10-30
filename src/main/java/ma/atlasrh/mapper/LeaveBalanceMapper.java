package ma.atlasrh.mapper;

import ma.atlasrh.dto.response.LeaveBalanceResponse;
import ma.atlasrh.entity.LeaveType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveBalanceMapper {

    @Mapping(source = "name", target = "leaveType")
    @Mapping(source = "daysPerYear", target = "annualDays")
    @Mapping(target = "usedDays", ignore = true)
    @Mapping(target = "remainingDays", ignore = true)
    LeaveBalanceResponse toResponse(LeaveType leaveType);
}
