package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.request.DepartmentRequest;
import ma.atlasrh.dto.response.DepartmentResponse;
import ma.atlasrh.dto.response.MessageResponse;
import ma.atlasrh.service.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Départements", description = "Gestion des départements de l'entreprise.")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @Operation(summary = "Créer un département",
            description = "Crée un nouveau département. Le nom doit être unique (409 sinon).")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un département",
            description = "Met à jour le nom et la description d'un département existant (404 si introuvable, 409 si le nom est déjà pris).")
    public DepartmentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un département",
            description = "Retourne le détail d'un département (404 s'il n'existe pas).")
    public DepartmentResponse getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Lister les départements",
            description = "Retourne la liste paginée des départements, triée par nom par défaut.")
    public Page<DepartmentResponse> getAll(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return departmentService.getAll(pageable);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un département",
            description = "Supprime un département vide. Un département contenant des employés ne peut pas être supprimé (409).")
    public MessageResponse delete(@PathVariable Long id) {
        return departmentService.delete(id);
    }
}
