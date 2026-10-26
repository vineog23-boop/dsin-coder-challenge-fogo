package br.com.dsin.challengefogo.equipment.web;

import br.com.dsin.challengefogo.equipment.application.EquipmentService;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentFilter;
import br.com.dsin.challengefogo.equipment.application.dto.HistoryEventDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.StatusChangeDtoRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Perfis por rota ficam no {@code SecurityConfig}: leitura para ambos, escrita só para o gestor. */
@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<EquipmentDtoResponse> create(@RequestBody @Valid EquipmentDtoRequest request) {
        EquipmentDtoResponse created = equipmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<EquipmentDtoResponse> search(EquipmentFilter filter,
                                             @PageableDefault(size = 20, sort = "createdAt",
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        return equipmentService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public EquipmentDtoResponse findById(@PathVariable UUID id) {
        return equipmentService.findById(id);
    }

    @PutMapping("/{id}")
    public EquipmentDtoResponse update(@PathVariable UUID id, @RequestBody @Valid EquipmentDtoRequest request) {
        return equipmentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public EquipmentDtoResponse changeStatus(@PathVariable UUID id, @RequestBody @Valid StatusChangeDtoRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        return equipmentService.changeStatus(id, request, jwt.getSubject());
    }

    @PostMapping("/{id}/maintenances")
    public ResponseEntity<MaintenanceDtoResponse> registerMaintenance(@PathVariable UUID id,
                                                                      @RequestBody @Valid MaintenanceDtoRequest request,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        MaintenanceDtoResponse created = equipmentService.registerMaintenance(id, request, jwt.getSubject());

        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}/history")
    public Page<HistoryEventDtoResponse> history(@PathVariable UUID id,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return equipmentService.history(id, pageable);
    }
}
