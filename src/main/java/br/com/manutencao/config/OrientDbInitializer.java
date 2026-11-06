package br.com.manutencao.config;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OrientDbInitializer implements CommandLineRunner {
    private final ODatabasePool pool;

    public OrientDbInitializer(ODatabasePool pool) {
        this.pool = pool;
    }

    @Override
    public void run(String... args) {
        try (ODatabaseSession db = pool.acquire()) {
            OClass equipment = ensureClass(db, "Equipment");
            addProperty(equipment, "name", OType.STRING);
            addProperty(equipment, "category", OType.STRING);
            addProperty(equipment, "brand", OType.STRING);
            addProperty(equipment, "model", OType.STRING);
            addProperty(equipment, "assetTag", OType.STRING);
            addProperty(equipment, "location", OType.STRING);
            addProperty(equipment, "status", OType.STRING);
            addProperty(equipment, "description", OType.STRING);

            OClass maintenance = ensureClass(db, "Maintenance");
            addProperty(maintenance, "equipmentId", OType.STRING);
            addProperty(maintenance, "title", OType.STRING);
            addProperty(maintenance, "description", OType.STRING);
            addProperty(maintenance, "priority", OType.STRING);
            addProperty(maintenance, "status", OType.STRING);
            addProperty(maintenance, "createdAt", OType.STRING);
            addProperty(maintenance, "completedAt", OType.STRING);
        }
    }

    private OClass ensureClass(ODatabaseSession db, String name) {
        OClass existing = db.getClass(name);
        return existing == null ? db.createClass(name) : existing;
    }

    private void addProperty(OClass type, String name, OType dataType) {
        if (type.getProperty(name) == null) {
            type.createProperty(name, dataType);
        }
    }
}
