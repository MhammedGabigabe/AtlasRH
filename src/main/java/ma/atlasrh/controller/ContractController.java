package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.request.ContractRequest;
import ma.atlasrh.dto.response.ContractResponse;
import ma.atlasrh.service.ContractService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Contrats", description = "Gestion des contrats de travail des employés.")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/employees/{employeeId}/contracts")
    @Operation(summary = "Créer un contrat",
            description = "Crée un contrat EN_COURS pour un employé. Un seul contrat en cours par employé (409). Un CDD ou STAGE doit avoir une date de fin (400).")
    public ResponseEntity<ContractResponse> create(@PathVariable Long employeeId,
                                                   @Valid @RequestBody ContractRequest request) {
        ContractResponse response = contractService.create(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/contracts/{contractId}/terminate")
    @Operation(summary = "Terminer un contrat",
            description = "Passe le contrat au statut TERMINE et fixe sa date de fin à aujourd'hui. Un contrat déjà terminé renvoie 409.")
    public ContractResponse terminate(@PathVariable Long contractId) {
        return contractService.terminate(contractId);
    }

    @GetMapping("/employees/{employeeId}/contracts")
    @Operation(summary = "Consulter l'historique des contrats",
            description = "Retourne tous les contrats d'un employé, du plus récent au plus ancien (404 si l'employé n'existe pas).")
    public List<ContractResponse> getHistory(@PathVariable Long employeeId) {
        return contractService.getHistory(employeeId);
    }
}
