package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.request.EmployeeRequest;
import ma.atlasrh.dto.response.EmployeeResponse;
import ma.atlasrh.dto.response.EmployeeSeniorityResponse;
import ma.atlasrh.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employés", description = "Gestion des employés et de leurs managers.")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @Operation(summary = "Créer un employé",
            description = "Crée un employé rattaché à un département existant. Le matricule et l'e-mail doivent être uniques (409).")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un employé",
            description = "Met à jour un employé. Un employé ne peut pas être son propre manager (400). Matricule et e-mail restent uniques (409).")
    public EmployeeResponse update(@PathVariable Long id,
                                   @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un employé",
            description = "Retourne le détail d'un employé (404 s'il n'existe pas).")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Rechercher des employés",
            description = "Recherche paginée sur le nom ou le prénom (insensible à la casse), avec un filtre optionnel par département.")
    public Page<EmployeeResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 10, sort = "lastName") Pageable pageable) {
        return employeeService.search(search, departmentId, pageable);
    }

    @GetMapping("/me/seniority")
    @Operation(summary = "Consulter mon ancienneté")
    public EmployeeSeniorityResponse getMySeniority(
            @RequestHeader("X-Employee-Id") Long employeeId) {
        return employeeService.getMySeniority(employeeId);
    }

    @GetMapping("/{id}/team")
    @Operation(summary = "Consulter l'équipe d'un manager",
            description = "Retourne les employés dont le manager est l'employé indiqué (404 si le manager n'existe pas).")
    public Page<EmployeeResponse> getTeam(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "lastName") Pageable pageable) {
        return employeeService.getTeam(id, pageable);
    }
}
