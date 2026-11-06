package br.com.manutencao.equipment;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MaintenanceService {
    private final ODatabasePool pool;
    private final EquipmentService equipmentService;

    public MaintenanceService(ODatabasePool pool, EquipmentService equipmentService) {
        this.pool = pool;
        this.equipmentService = equipmentService;
    }

    public List<Maintenance> findAll(String equipmentId) {
        List<Maintenance> result = new ArrayList<>();
        try (ODatabaseSession db = pool.acquire()) {
            for (ODocument document : db.browseClass("Maintenance")) {
                String ownerId = document.field("equipmentId");
                if (equipmentId == null || equipmentId.equals(ownerId)) {
                    result.add(toMaintenance(document, db));
                }
            }
        }
        result.sort(Comparator.comparing(Maintenance::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public Maintenance create(String equipmentId, Maintenance input) {
        if (input == null || input.getTitle() == null || input.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o título do chamado.");
        }
        try (ODatabaseSession db = pool.acquire()) {
            ODocument equipment = equipmentService.loadEquipment(db, equipmentId);
            ODocument document = new ODocument("Maintenance");
            document.field("equipmentId", equipment.getIdentity().toString());
            document.field("title", input.getTitle().trim());
            document.field("description", clean(input.getDescription()));
            document.field("priority", normalizePriority(input.getPriority()));
            document.field("status", "Aberto");
            document.field("createdAt", LocalDateTime.now().toString());
            document.field("completedAt", "");
            db.save(document);
            equipment.field("status", "Em manutenção");
            db.save(equipment);
            return toMaintenance(document, db);
        }
    }

    public Maintenance complete(String id) {
        try (ODatabaseSession db = pool.acquire()) {
            ODocument document = loadMaintenance(db, id);
            document.field("status", "Concluído");
            document.field("completedAt", LocalDateTime.now().toString());
            db.save(document);

            String equipmentId = document.field("equipmentId");
            boolean hasOpenItems = false;
            for (ODocument item : db.browseClass("Maintenance")) {
                if (equipmentId.equals(item.field("equipmentId")) && "Aberto".equals(item.field("status"))) {
                    hasOpenItems = true;
                    break;
                }
            }
            if (!hasOpenItems) {
                ODocument equipment = equipmentService.loadEquipment(db, equipmentId);
                equipment.field("status", "Em uso");
                db.save(equipment);
            }
            return toMaintenance(document, db);
        }
    }

    private ODocument loadMaintenance(ODatabaseSession db, String id) {
        try {
            if (id == null || !id.matches("#?\\d+:\\d+")) throw new IllegalArgumentException();
            ODocument document = db.load(new com.orientechnologies.orient.core.id.ORecordId(id));
            if (document == null || !"Maintenance".equals(document.getClassName())) throw new IllegalArgumentException();
            return document;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado de manutenção não encontrado.");
        }
    }

    private Maintenance toMaintenance(ODocument document, ODatabaseSession db) {
        Maintenance item = new Maintenance();
        item.setId(document.getIdentity().toString());
        item.setEquipmentId(document.field("equipmentId"));
        item.setTitle(document.field("title"));
        item.setDescription(document.field("description"));
        item.setPriority(document.field("priority"));
        item.setStatus(document.field("status"));
        item.setCreatedAt(document.field("createdAt"));
        item.setCompletedAt(document.field("completedAt"));
        try {
            ODocument equipment = equipmentService.loadEquipment(db, item.getEquipmentId());
            item.setEquipmentName(equipment.field("name"));
        } catch (ResponseStatusException ignored) {
            item.setEquipmentName("Equipamento removido");
        }
        return item;
    }

    private String normalizePriority(String priority) {
        if ("Baixa".equals(priority) || "Média".equals(priority) || "Alta".equals(priority)) return priority;
        return "Média";
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
}
