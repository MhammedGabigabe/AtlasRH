package ma.atlasrh.repository;

import ma.atlasrh.entity.Contract;
import ma.atlasrh.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByEmployeeIdAndStatus(Long employeeId, ContractStatus status);

    List<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId);
}
