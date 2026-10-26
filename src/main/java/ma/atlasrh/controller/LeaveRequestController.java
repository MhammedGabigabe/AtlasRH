package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.request.LeaveRequestCreateRequest;
import ma.atlasrh.dto.response.LeaveRequestResponse;
import ma.atlasrh.service.LeaveRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
}
