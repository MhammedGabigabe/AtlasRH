package ma.atlasrh.repository;

import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId,
                                                   LeaveRequestStatus status,
                                                   Pageable pageable);

    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.employee.manager.id = :managerId
              AND r.status = :status
            """)
    Page<LeaveRequest> findByTeamManagerIdAndStatus(@Param("managerId") Long managerId,
                                                     @Param("status") LeaveRequestStatus status,
                                                     Pageable pageable);

    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, Collection<LeaveRequestStatus> statuses,
            LocalDate endDate, LocalDate startDate);

    @Query("""
            SELECT COALESCE(SUM(r.numberOfDays), 0) FROM LeaveRequest r
            WHERE r.employee.id = :employeeId
              AND r.leaveType.id = :leaveTypeId
              AND r.status IN :statuses
            """)
    long sumDaysByEmployeeAndLeaveTypeAndStatuses(@Param("employeeId") Long employeeId,
                                                   @Param("leaveTypeId") Long leaveTypeId,
                                                   @Param("statuses") Collection<LeaveRequestStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(r.numberOfDays), 0) FROM LeaveRequest r
            WHERE r.employee.id = :employeeId
              AND r.leaveType.id = :leaveTypeId
              AND r.status = :status
              AND r.startDate >= :yearStart
              AND r.startDate < :nextYearStart
            """)
    long sumApprovedDaysForYear(@Param("employeeId") Long employeeId,
                                @Param("leaveTypeId") Long leaveTypeId,
                                @Param("status") LeaveRequestStatus status,
                                @Param("yearStart") LocalDate yearStart,
                                @Param("nextYearStart") LocalDate nextYearStart);
}
