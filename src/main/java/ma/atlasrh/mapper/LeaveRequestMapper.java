package ma.atlasrh.mapper;

import ma.atlasrh.dto.request.LeaveRequestCreateRequest;
import ma.atlasrh.dto.response.LeaveRequestResponse;
import ma.atlasrh.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    @Mapping(target = "numberOfDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "managerComment", ignore = true)
    LeaveRequest toEntity(LeaveRequestCreateRequest request);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "leaveType.id", target = "leaveTypeId")
    LeaveRequestResponse toResponse(LeaveRequest request);
}
