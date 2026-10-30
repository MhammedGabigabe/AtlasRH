package ma.atlasrh.service;

import ma.atlasrh.dto.response.LeaveBalanceResponse;
import ma.atlasrh.entity.LeaveType;
import ma.atlasrh.enums.LeaveRequestStatus;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.LeaveBalanceMapper;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import ma.atlasrh.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveBalanceService {

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceMapper leaveBalanceMapper;

    public LeaveBalanceService(EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveRequestRepository leaveRequestRepository,
                               LeaveBalanceMapper leaveBalanceMapper) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceMapper = leaveBalanceMapper;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> findMine(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(
                    "Employé introuvable avec l'id " + employeeId + ".");
        }

        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        LocalDate nextYearStart = yearStart.plusYears(1);

        return leaveTypeRepository.findAllByOrderByNameAsc().stream()
                .map(leaveType -> toBalance(employeeId, leaveType, yearStart, nextYearStart))
                .toList();
    }

    private LeaveBalanceResponse toBalance(Long employeeId,
                                           LeaveType leaveType,
                                           LocalDate yearStart,
                                           LocalDate nextYearStart) {
        long usedDays = leaveRequestRepository.sumApprovedDaysForYear(
                employeeId, leaveType.getId(), LeaveRequestStatus.APPROUVEE,
                yearStart, nextYearStart);
        LeaveBalanceResponse baseResponse = leaveBalanceMapper.toResponse(leaveType);
        return new LeaveBalanceResponse(
                baseResponse.leaveType(),
                baseResponse.annualDays(),
                usedDays,
                baseResponse.annualDays() - usedDays);
    }
}
