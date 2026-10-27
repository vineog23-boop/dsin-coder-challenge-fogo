package br.com.dsin.challengefogo.operations.web;

import br.com.dsin.challengefogo.operations.application.AlertService;
import br.com.dsin.challengefogo.operations.application.OperationsService;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.MapDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.MapFilter;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Central de operações: só leitura, para os dois perfis (regra de {@code GET /api/**} do {@code SecurityConfig}). */
@RestController
@RequestMapping("/api/operations")
public class OperationsController {

    private final OperationsService operationsService;
    private final AlertService alertService;

    public OperationsController(OperationsService operationsService, AlertService alertService) {
        this.operationsService = operationsService;
        this.alertService = alertService;
    }

    @GetMapping("/summary")
    public SummaryDtoResponse summary() {
        return operationsService.summary();
    }

    @GetMapping("/map")
    public MapDtoResponse map(MapFilter filter) {
        return operationsService.map(filter);
    }

    @GetMapping("/alerts")
    public Page<AlertDtoResponse> alerts(@PageableDefault(size = 20) Pageable pageable) {
        return alertService.alerts(pageable);
    }
}
