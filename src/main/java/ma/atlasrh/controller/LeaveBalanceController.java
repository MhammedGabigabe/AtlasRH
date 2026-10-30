package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.atlasrh.dto.response.LeaveBalanceResponse;
import ma.atlasrh.service.LeaveBalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
@Tag(name = "Soldes de congé")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consulter mon solde de congés",
            description = "Calcule le solde de chaque type de congé pour l'employé identifié par X-Employee-Id.")
    public List<LeaveBalanceResponse> findMine(
            @RequestHeader("X-Employee-Id") Long employeeId) {
        return leaveBalanceService.findMine(employeeId);
    }
}
