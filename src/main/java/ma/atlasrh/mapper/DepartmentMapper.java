package ma.atlasrh.mapper;

import ma.atlasrh.dto.request.DepartmentRequest;
import ma.atlasrh.dto.response.DepartmentResponse;
import ma.atlasrh.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    @Mapping(target = "id", ignore = true)
    Department toEntity(DepartmentRequest request);

    DepartmentResponse toResponse(Department department);

    @Mapping(target = "id", ignore = true)
    void updateEntity(DepartmentRequest request, @MappingTarget Department department);
}
