package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.GeneratorProcessException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

public class AccessTypeMetadataGenerator implements MetadataGenerator {
    
    private String NAME = "accessTypeMetadataGenerator";
    private List<String> acceptedEntityTypes;
    private MetadataField accessTypeField;

    @Autowired
    private AccessStatusService accessStatusService;

    @Autowired
    private ItemService itemService;

    @Override
    public String getGeneratorName() {
        return this.NAME;
    }

    /**
     * Retrieve all the access types of the item and generate the global access type from them.
     */
    @Override
    public void process(Context context, Item item) throws GeneratorProcessException {
        try {
            String accessValue = accessStatusService.getAccessStatus(context, item);
            if (accessValue != null) {
                this.itemService.setMetadataSingleValue(context, item,
                        this.accessTypeField.getSchema(),
                        this.accessTypeField.getElement(),
                        this.accessTypeField.getQualifier(),
                        null,
                        accessValue
                );
            }
        } catch (SQLException e) {
            throw new GeneratorProcessException("An error occurred while updating access type metadata", e);
        } catch (Exception e) {
            throw new GeneratorProcessException("An unhandled exception occurred", e);
        }
    }

    /**
     * Check if the item can be processed by this generator.
     * 
     * @param ctx: The DSpace context.
     * @param item: The item to process.
     */
    @Override
    public Boolean canBeProcessed(Context ctx, Item item) {
        String currentEntityType = item.getItemService().getEntityType(item);
        return currentEntityType != null && this.acceptedEntityTypes.contains(currentEntityType);
    }

    // Getters && Setters

    public List<String> getAcceptedEntityTypes() {
        return this.acceptedEntityTypes;
    }

    public void setAcceptedEntityTypes(List<String> acceptedEntityTypes) {
        this.acceptedEntityTypes = acceptedEntityTypes;
    }

    public MetadataField getAccessTypeField() {
        return this.accessTypeField;
    }

    public void setAccessTypeField(MetadataField accessTypeField) {
        this.accessTypeField = accessTypeField;
    }
}
