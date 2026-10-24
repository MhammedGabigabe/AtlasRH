package ma.atlasrh.mapper;

import ma.atlasrh.dto.request.ContractRequest;
import ma.atlasrh.dto.response.ContractResponse;
import ma.atlasrh.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "status", ignore = true)
    Contract toEntity(ContractRequest request);

    @Mapping(source = "employee.id", target = "employeeId")
    ContractResponse toResponse(Contract contract);
}
