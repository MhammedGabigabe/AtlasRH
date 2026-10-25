package ma.atlasrh.service;

import ma.atlasrh.dto.request.EmployeeRequest;
import ma.atlasrh.dto.response.EmployeeResponse;
import ma.atlasrh.entity.Department;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.exception.BadRequestException;
import ma.atlasrh.exception.DuplicateResourceException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.EmployeeMapper;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.employeeMapper = employeeMapper;
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        checkDuplicateMatricule(request.matricule(), null);
        checkDuplicateEmail(request.email(), null);

        Department department = findDepartment(request.departmentId());
        Employee manager = findManager(request.managerId());

        Employee employee = employeeMapper.toEntity(request);
        employee.setDepartment(department);
        employee.setManager(manager);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employé créé avec l'id {} et le matricule {}", savedEmployee.getId(), savedEmployee.getMatricule());

        return employeeMapper.toResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEmployee(id);

        if (request.managerId() != null && request.managerId().equals(id)) {
            throw new BadRequestException("Un employé ne peut pas être son propre manager.");
        }

        checkDuplicateMatricule(request.matricule(), id);
        checkDuplicateEmail(request.email(), id);

        Department department = findDepartment(request.departmentId());
        Employee manager = findManager(request.managerId());

        employeeMapper.updateEntity(request, employee);
        employee.setDepartment(department);
        employee.setManager(manager);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employé {} mis à jour", savedEmployee.getId());

        return employeeMapper.toResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        Employee employee = findEmployee(id);
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(String search, Long departmentId, Pageable pageable) {
        // PostgreSQL n'aime pas un paramètre null dans un LIKE : on utilise une chaîne vide.
        String searchTerm = search == null ? "" : search;

        return employeeRepository.search(searchTerm, departmentId, pageable)
                .map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getTeam(Long managerId, Pageable pageable) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager introuvable avec l'id " + managerId + ".");
        }

        return employeeRepository.findByManagerId(managerId, pageable)
                .map(employeeMapper::toResponse);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé introuvable avec l'id " + id + "."));
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département introuvable avec l'id " + departmentId + "."));
    }

    private Employee findManager(Long managerId) {
        if (managerId == null) {
            return null;
        }

        return employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager introuvable avec l'id " + managerId + "."));
    }

    private void checkDuplicateMatricule(String matricule, Long id) {
        boolean exists = id == null
                ? employeeRepository.existsByMatricule(matricule)
                : employeeRepository.existsByMatriculeAndIdNot(matricule, id);

        if (exists) {
            throw new DuplicateResourceException(
                    "Un employé avec le matricule '" + matricule + "' existe déjà.");
        }
    }

    private void checkDuplicateEmail(String email, Long id) {
        boolean exists = id == null
                ? employeeRepository.existsByEmail(email)
                : employeeRepository.existsByEmailAndIdNot(email, id);

        if (exists) {
            throw new DuplicateResourceException(
                    "Un employé avec l'e-mail '" + email + "' existe déjà.");
        }
    }
}
