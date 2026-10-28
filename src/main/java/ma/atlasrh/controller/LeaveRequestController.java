package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.request.LeaveRequestCreateRequest;
import ma.atlasrh.dto.response.LeaveRequestResponse;
import ma.atlasrh.enums.LeaveRequestStatus;
import ma.atlasrh.service.LeaveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave-requests")
@Tag(name = "Demandes de congé")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    @Operation(summary = "Créer une demande de congé",
            description = "L'employé est identifié par l'en-tête X-Employee-Id.")
    public ResponseEntity<LeaveRequestResponse> create(
            @RequestHeader("X-Employee-Id") Long employeeId,
            @Valid @RequestBody LeaveRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveRequestService.create(employeeId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consulter mes demandes de congé",
            description = "Retourne uniquement les demandes de l'employé identifié par X-Employee-Id, avec un filtre optionnel par statut.")
    public Page<LeaveRequestResponse> findMine(
            @RequestHeader("X-Employee-Id") Long employeeId,
            @RequestParam(required = false) LeaveRequestStatus status,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return leaveRequestService.findMine(employeeId, status, pageable);
    }

    @GetMapping("/team/pending")
    @Operation(summary = "Consulter les demandes en attente de mon équipe",
            description = "Retourne uniquement les demandes EN_ATTENTE des employés rattachés au manager identifié par X-Employee-Id.")
    public Page<LeaveRequestResponse> findPendingTeam(
            @RequestHeader("X-Employee-Id") Long managerId,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return leaveRequestService.findPendingTeam(managerId, pageable);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Annuler une demande de congé",
            description = "Annule une demande appartenant à l'employé identifié par X-Employee-Id, uniquement si elle est EN_ATTENTE.")
    public LeaveRequestResponse cancel(
            @RequestHeader("X-Employee-Id") Long employeeId,
            @PathVariable Long id) {
        return leaveRequestService.cancel(employeeId, id);
    }
}
