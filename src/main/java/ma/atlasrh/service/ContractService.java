package ma.atlasrh.service;

import ma.atlasrh.dto.request.ContractRequest;
import ma.atlasrh.dto.response.ContractResponse;
import ma.atlasrh.entity.Contract;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.enums.ContractStatus;
import ma.atlasrh.enums.ContractType;
import ma.atlasrh.exception.BadRequestException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.ContractMapper;
import ma.atlasrh.repository.ContractRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractMapper contractMapper;

    public ContractService(ContractRepository contractRepository,
                           EmployeeRepository employeeRepository,
                           ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.contractMapper = contractMapper;
    }

    @Transactional
    public ContractResponse create(Long employeeId, ContractRequest request) {
        Employee employee = findEmployee(employeeId);
        validateDates(request);

        if (contractRepository.existsByEmployeeIdAndStatus(employeeId, ContractStatus.EN_COURS)) {
            throw new ConflictException(
                    "L'employé '" + employee.getMatricule() + "' a déjà un contrat en cours.");
        }

        Contract contract = contractMapper.toEntity(request);
        contract.setEmployee(employee);
        contract.setStatus(ContractStatus.EN_COURS);

        Contract savedContract = contractRepository.save(contract);
        log.info("Contrat {} créé pour l'employé {}", savedContract.getId(), employee.getMatricule());

        return contractMapper.toResponse(savedContract);
    }

    @Transactional
    public ContractResponse terminate(Long contractId) {
        Contract contract = findContract(contractId);

        if (contract.getStatus() == ContractStatus.TERMINE) {
            throw new ConflictException("Le contrat " + contractId + " est déjà terminé.");
        }

        contract.setStatus(ContractStatus.TERMINE);
        contract.setEndDate(LocalDate.now());

        Contract savedContract = contractRepository.save(contract);
        log.info("Contrat {} terminé le {}", savedContract.getId(), savedContract.getEndDate());

        return contractMapper.toResponse(savedContract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé introuvable avec l'id " + employeeId + ".");
        }

        return contractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId).stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    private void validateDates(ContractRequest request) {
        if ((request.type() == ContractType.CDD || request.type() == ContractType.STAGE)
                && request.endDate() == null) {
            throw new BadRequestException("Un contrat CDD ou STAGE doit avoir une date de fin.");
        }

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException(
                    "La date de fin ne peut pas être antérieure à la date de début.");
        }
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé introuvable avec l'id " + employeeId + "."));
    }

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrat introuvable avec l'id " + contractId + "."));
    }
}
