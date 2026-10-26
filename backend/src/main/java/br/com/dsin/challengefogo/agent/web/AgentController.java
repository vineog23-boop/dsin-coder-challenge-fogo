package br.com.dsin.challengefogo.agent.web;

import br.com.dsin.challengefogo.agent.application.AgentService;
import br.com.dsin.challengefogo.agent.application.dto.AgentDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.AgentDtoResponse;
import br.com.dsin.challengefogo.agent.application.dto.AgentFilter;
import br.com.dsin.challengefogo.agent.application.dto.AgentHistoryDtoResponse;
import br.com.dsin.challengefogo.agent.application.dto.AgentStatusChangeDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.PositionDtoRequest;
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

/** Perfis por rota no {@code SecurityConfig}: o {@code PATCH /position} é a única escrita do operador. */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<AgentDtoResponse> create(@RequestBody @Valid AgentDtoRequest request) {
        AgentDtoResponse created = agentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<AgentDtoResponse> search(AgentFilter filter,
                                         @PageableDefault(size = 20, sort = "name",
                                                 direction = Sort.Direction.ASC) Pageable pageable) {
        return agentService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public AgentDtoResponse findById(@PathVariable UUID id) {
        return agentService.findById(id);
    }

    @PutMapping("/{id}")
    public AgentDtoResponse update(@PathVariable UUID id, @RequestBody @Valid AgentDtoRequest request) {
        return agentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public AgentDtoResponse changeStatus(@PathVariable UUID id,
                                         @RequestBody @Valid AgentStatusChangeDtoRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        return agentService.changeStatus(id, request, jwt.getSubject());
    }

    @PatchMapping("/{id}/position")
    public AgentDtoResponse updatePosition(@PathVariable UUID id, @RequestBody @Valid PositionDtoRequest request) {
        return agentService.updatePosition(id, request);
    }

    @GetMapping("/{id}/history")
    public Page<AgentHistoryDtoResponse> history(@PathVariable UUID id, @PageableDefault(size = 20) Pageable pageable) {
        return agentService.history(id, pageable);
    }
}
