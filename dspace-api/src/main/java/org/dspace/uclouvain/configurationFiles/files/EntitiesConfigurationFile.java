package org.dspace.uclouvain.configurationFiles.files;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.configurationFiles.AbstractConfigurationFile;
import org.dspace.uclouvain.core.model.Entity;
import org.dspace.uclouvain.core.model.EntityType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class EntitiesConfigurationFile extends AbstractConfigurationFile<Set<Entity>> {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(EntitiesConfigurationFile.class);

    // CONSTRUCTOR ============================================================
    public EntitiesConfigurationFile(String filePath) throws IOException {
        super(filePath);
    }

    // METHODS ================================================================
    @Override
    public void loadData() {
        try {
            this.data = new TreeSet<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode entitiesData = mapper.readTree(this.getRawData());
            for (JsonNode entityData : entitiesData) {
                this.data.addAll(parseEntity(entityData, null));
            }
        } catch (IOException e) {
            log.error("Unable to load entities from configuration file : " + e.getMessage());
        }

    }

    /**
     * Recursively load an entity and all potential children entities
     *
     * @param entityData : the entity json node data
     * @param parent     : the parent entity (could be null)
     * @return the list of entities loaded from an entity node.
     */
    private static List<Entity> parseEntity(JsonNode entityData, @Nullable Entity parent) {
        List<Entity> entities = new ArrayList<>();
        Entity entity = new Entity(
            entityData.get("code").asText(),
            entityData.get("name").asText(),
            EntityType.fromLabel(entityData.get("type").asText())
        );
        entity.setParent(parent);
        if (parent != null) {
            parent.addChild(entity);
        }
        entities.add(entity);
        if (entityData.has("children")) {
            for (JsonNode childEntityData : entityData.get("children")) {
                entities.addAll(parseEntity(childEntityData, entity));
            }
        }
        return entities;
    }
}
