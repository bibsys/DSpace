package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.GeneratorProcessException;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.MalformedDegreeCodeException;
import org.springframework.beans.factory.annotation.Autowired;

public class DegreeMetadataGenerator implements MetadataGenerator {
    
    @Autowired
    ItemService itemService;

    public String NAME = "degreeMetadataGenerator";

    private MetadataField degreeCodeField;

    private MetadataField degreeLabelField;

    @Override
    public String getGeneratorName() {
        return this.NAME;
    }

    @Override
    public void process(Context ctx, Item item) throws GeneratorProcessException {
        try {
            ItemService currentItemService = item.getItemService();
            List<MetadataValue> currentDegreeCodes = currentItemService.getMetadata(item, this.degreeCodeField.getSchema(), this.degreeCodeField.getElement(), this.degreeCodeField.getQualifier(), null);
            for (MetadataValue mv: currentDegreeCodes) {
                List<String> newValues = this.generateNewDegreeCode(mv);
                // Replace degree code
                currentItemService.replaceSecuredMetadata(ctx, item, this.degreeCodeField.getSchema(), this.degreeCodeField.getElement(), this.degreeCodeField.getQualifier(), null, newValues.get(0), null, -1, mv.getPlace(), 0);
                // Replace degree label
                currentItemService.addMetadata(ctx, item, this.degreeLabelField.getSchema(), this.degreeLabelField.getElement(), this.degreeLabelField.getQualifier(), null, newValues.get(1), null, -1, 0);
            }
            currentItemService.update(ctx, item);
        } catch (SQLException e) {
            throw new GeneratorProcessException("An error occurred while replacing degree metadata", e);
        } catch (MalformedDegreeCodeException e) {
            throw new GeneratorProcessException("Could not generate degree code and label due to malformed degree code", e);
        } catch (AuthorizeException e) {
            throw new GeneratorProcessException("Not authorized to update the item", e);
        }
    }

    private List<String> generateNewDegreeCode(MetadataValue mv) throws MalformedDegreeCodeException {
        List<String> choppedList = Arrays.asList(mv.getValue().split("-"));
        if (choppedList.size() > 1) {
            return choppedList;
        }
        throw new MalformedDegreeCodeException("Error while generating new degree code && label");
    }

    public MetadataField getDegreeCodeField(){
        return this.degreeCodeField;
    }

    public void setDegreeCodeField(MetadataField degreeCodeField){
        this.degreeCodeField = degreeCodeField;
    }

    public MetadataField getDegreeLabelField(){
        return this.degreeLabelField;
    }

    public void setDegreeLabelField(MetadataField degreeLabelField){
        this.degreeLabelField = degreeLabelField;
    }
}
