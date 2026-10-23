package ma.atlasrh.repository;

import ma.atlasrh.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);

    boolean existsByMatriculeAndIdNot(String matricule, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    Page<Employee> findByManagerId(Long managerId, Pageable pageable);

    @Query("""
            SELECT e FROM Employee e
            WHERE (LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
            """)
    Page<Employee> search(@Param("search") String search,
                          @Param("departmentId") Long departmentId,
                          Pageable pageable);
}
