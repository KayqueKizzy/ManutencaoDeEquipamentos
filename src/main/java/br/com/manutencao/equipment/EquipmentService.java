package br.com.manutencao.equipment;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class EquipmentService {
    private static final List<String> STATUSES = Arrays.asList("Em uso", "Em manutenção", "Inativo");
    private final ODatabasePool pool;

    public EquipmentService(ODatabasePool pool) {
        this.pool = pool;
    }

    public List<Equipment> findAll() {
        List<Equipment> result = new ArrayList<>();
        try (ODatabaseSession db = pool.acquire()) {
            for (ODocument document : db.browseClass("Equipment")) {
                result.add(toEquipment(document));
            }
        }
        result.sort((a, b) -> safe(a.getName()).compareToIgnoreCase(safe(b.getName())));
        return result;
    }

    public Equipment findById(String id) {
        try (ODatabaseSession db = pool.acquire()) {
            return toEquipment(loadEquipment(db, id));
        }
    }

    public Equipment create(Equipment input) {
        validate(input);
        try (ODatabaseSession db = pool.acquire()) {
            ODocument document = new ODocument("Equipment");
            write(document, input);
            db.save(document);
            return toEquipment(document);
        }
    }

    public Equipment update(String id, Equipment input) {
        validate(input);
        try (ODatabaseSession db = pool.acquire()) {
            ODocument document = loadEquipment(db, id);
            write(document, input);
            db.save(document);
            return toEquipment(document);
        }
    }

    public void delete(String id) {
        try (ODatabaseSession db = pool.acquire()) {
            ODocument document = loadEquipment(db, id);
            db.delete(document);
        }
    }

    ODocument loadEquipment(ODatabaseSession db, String id) {
        try {
            if (id == null || !id.matches("#?\\d+:\\d+")) {
                throw new IllegalArgumentException();
            }
            ODocument document = db.load(new ORecordId(id));
            if (document == null || !"Equipment".equals(document.getClassName())) {
                throw new IllegalArgumentException();
            }
            return document;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado.");
        }
    }

    static Equipment toEquipment(ODocument document) {
        Equipment equipment = new Equipment();
        equipment.setId(document.getIdentity().toString());
        equipment.setName(document.field("name"));
        equipment.setCategory(document.field("category"));
        equipment.setBrand(document.field("brand"));
        equipment.setModel(document.field("model"));
        equipment.setAssetTag(document.field("assetTag"));
        equipment.setLocation(document.field("location"));
        equipment.setStatus(document.field("status"));
        equipment.setDescription(document.field("description"));
        return equipment;
    }

    private void write(ODocument document, Equipment equipment) {
        document.field("name", equipment.getName().trim());
        document.field("category", equipment.getCategory().trim());
        document.field("brand", clean(equipment.getBrand()));
        document.field("model", clean(equipment.getModel()));
        document.field("assetTag", clean(equipment.getAssetTag()));
        document.field("location", clean(equipment.getLocation()));
        document.field("status", STATUSES.contains(equipment.getStatus()) ? equipment.getStatus() : "Em uso");
        document.field("description", clean(equipment.getDescription()));
    }

    private void validate(Equipment equipment) {
        if (equipment == null || equipment.getName() == null || equipment.getName().trim().isEmpty()
                || equipment.getCategory() == null || equipment.getCategory().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome e a categoria do equipamento.");
        }
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String safe(String value) { return value == null ? "" : value; }
}
