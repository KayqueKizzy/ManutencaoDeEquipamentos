package br.com.manutencao.equipment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
public class MaintenanceController {
    private final MaintenanceService service;

    public MaintenanceController(MaintenanceService service) { this.service = service; }

    @GetMapping
    public List<Maintenance> findAll(@RequestParam(required = false) String equipamentoId) {
        return service.findAll(equipamentoId);
    }

    @PostMapping("/equipamento/{equipmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Maintenance create(@PathVariable String equipmentId, @Valid @RequestBody Maintenance maintenance) {
        return service.create(equipmentId, maintenance);
    }

    @PostMapping("/{id}/concluir")
    public Maintenance complete(@PathVariable String id) { return service.complete(id); }
}
