package ma.atlasrh.service;

import ma.atlasrh.dto.request.DepartmentRequest;
import ma.atlasrh.dto.response.DepartmentResponse;
import ma.atlasrh.dto.response.MessageResponse;
import ma.atlasrh.entity.Department;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.DuplicateResourceException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.DepartmentMapper;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository,
                             DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.departmentMapper = departmentMapper;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Un département avec le nom '" + request.name() + "' existe déjà.");
        }

        Department department = departmentMapper.toEntity(request);
        Department savedDepartment = departmentRepository.save(department);
        log.info("Département créé avec l'id {}", savedDepartment.getId());

        return departmentMapper.toResponse(savedDepartment);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findDepartment(id);

        if (departmentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(
                    "Un département avec le nom '" + request.name() + "' existe déjà.");
        }

        departmentMapper.updateEntity(request, department);
        Department savedDepartment = departmentRepository.save(department);
        log.info("Département {} mis à jour", savedDepartment.getId());

        return departmentMapper.toResponse(savedDepartment);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department department = findDepartment(id);
        return departmentMapper.toResponse(department);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAll(Pageable pageable) {
        return departmentRepository.findAll(pageable)
                .map(departmentMapper::toResponse);
    }

    @Transactional
    public MessageResponse delete(Long id) {
        Department department = findDepartment(id);

        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException(
                    "Impossible de supprimer le département '" + department.getName()
                            + "' car il contient encore des employés.");
        }

        departmentRepository.delete(department);
        log.info("Département {} supprimé", id);

        return new MessageResponse("Le département '" + department.getName() + "' a été supprimé avec succès.");
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département introuvable avec l'id " + id + "."));
    }
}
