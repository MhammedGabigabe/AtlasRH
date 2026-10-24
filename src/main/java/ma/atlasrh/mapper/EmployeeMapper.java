package ma.atlasrh.mapper;

import ma.atlasrh.dto.request.EmployeeRequest;
import ma.atlasrh.dto.response.EmployeeResponse;
import ma.atlasrh.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    Employee toEntity(EmployeeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    void updateEntity(EmployeeRequest request, @MappingTarget Employee employee);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "manager.id", target = "managerId")
    EmployeeResponse toResponse(Employee employee);
}
