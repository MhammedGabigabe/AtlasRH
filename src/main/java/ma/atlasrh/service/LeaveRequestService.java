package ma.atlasrh.service;

import ma.atlasrh.dto.request.LeaveRequestCreateRequest;
import ma.atlasrh.dto.response.LeaveRequestResponse;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.entity.LeaveType;
import ma.atlasrh.enums.LeaveRequestStatus;
import ma.atlasrh.exception.BadRequestException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.LeaveRequestMapper;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import ma.atlasrh.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Service
public class LeaveRequestService {

    private static final Set<LeaveRequestStatus> ACTIVE_STATUSES =
            EnumSet.of(LeaveRequestStatus.EN_ATTENTE, LeaveRequestStatus.APPROUVEE);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               EmployeeRepository employeeRepository,
                               LeaveRequestMapper leaveRequestMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestMapper = leaveRequestMapper;
    }

    @Transactional
    public LeaveRequestResponse create(Long employeeId, LeaveRequestCreateRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé introuvable avec l'id " + employeeId + "."));
        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Type de congé introuvable avec l'id " + request.leaveTypeId() + "."));

        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("La date de fin ne peut pas être antérieure à la date de début.");
        }

        int numberOfDays = countBusinessDays(request.startDate(), request.endDate());
        if (numberOfDays == 0) {
            throw new BadRequestException("La période doit contenir au moins un jour ouvré.");
        }

        if (leaveRequestRepository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId, ACTIVE_STATUSES, request.endDate(), request.startDate())) {
            throw new ConflictException("La période demandée chevauche un congé actif de l'employé.");
        }

        long alreadyUsed = leaveRequestRepository.sumDaysByEmployeeAndLeaveTypeAndStatuses(
                employeeId, leaveType.getId(), ACTIVE_STATUSES);
        if (alreadyUsed + numberOfDays > leaveType.getDaysPerYear()) {
            throw new ConflictException("Le solde disponible pour ce type de congé est insuffisant.");
        }

        LeaveRequest leaveRequest = leaveRequestMapper.toEntity(request);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setNumberOfDays(numberOfDays);
        leaveRequest.setStatus(LeaveRequestStatus.EN_ATTENTE);

        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> findMine(Long employeeId,
                                               LeaveRequestStatus status,
                                               Pageable pageable) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(
                    "Employé introuvable avec l'id " + employeeId + ".");
        }

        Page<LeaveRequest> requests = status == null
                ? leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                : leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);

        return requests.map(leaveRequestMapper::toResponse);
    }

    private int countBusinessDays(LocalDate startDate, LocalDate endDate) {
        int count = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }
}
