package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.GeneratorProcessException;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.MalformedDegreeCodeException;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.model.DegreeMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Generator in charge of generating degree metadata.
 * First it will split the degree code in two parts (degree code and label) and replace the degree code and label.
 * Then it will use the degree mappers service to retrieve additional information based on the degree code.
 * Finally it will add the additional information to the item.
 */
public class DegreeMetadataGenerator implements MetadataGenerator {

    @Autowired
    DegreeMappersService degreeMappersService;

    public String NAME = "degreeMetadataGenerator";

    private List<String> acceptedEntityTypes;

    private MetadataField degreeCodeField;

    private MetadataField degreeLabelField;

    private MetadataField rootDegreeCodeField;

    private MetadataField rootDegreeLabelField;

    private MetadataField facultyCodeField;

    private MetadataField facultyNameField;

    @Override
    public String getGeneratorName() {
        return this.NAME;
    }

    @Override
    public void process(Context ctx, Item item) throws GeneratorProcessException {
        try {
            ItemService currentItemService = item.getItemService();
            // 1) Clear previous metadata Values
            this.clearPreviousMetadata(ctx, item, currentItemService);

            // 2) Retrieve a list of distinct degree codes and labels splitted by '-'
            List<MetadataValue> currentDegreeCodes = currentItemService.getMetadata(
                item,
                this.degreeCodeField.getSchema(),
                this.degreeCodeField.getElement(),
                this.degreeCodeField.getQualifier(),
                null
            );
            // Generate code and labels in a list of pairs: [['code1', 'label1'], ['code2', 'label2'], ['code3', 'label3']]
            List<List<String>> codeAndLabels = this.generateCodeAndLabels(currentDegreeCodes);
            // Loop over the pairs and add the corresponding metadata to the item
            for (Integer elementIndex = 0; elementIndex < codeAndLabels.size(); elementIndex++) {
                List<String> values = codeAndLabels.get(elementIndex);
                // Replace degree code
                currentItemService.replaceSecuredMetadata(
                    ctx, item,
                    this.degreeCodeField.getSchema(),
                    this.degreeCodeField.getElement(),
                    this.degreeCodeField.getQualifier(),
                    null, values.get(0), null, -1, elementIndex, 0
                );
                // Add degree label
                currentItemService.addMetadata(
                    ctx, item,
                    this.degreeLabelField.getSchema(),
                    this.degreeLabelField.getElement(),
                    this.degreeLabelField.getQualifier(),
                    null, values.get(1), null, -1, 0
                );
            }

            // 3) Generate new information based on the distinct retrieved degree codes and the degree mappers file
            List<String> distinctCodes = codeAndLabels.stream().map(x -> x.get(0)).distinct().collect(Collectors.toList());
            List<DegreeMapper> mappings = this.degreeMappersService.getDegreeMappersForDegreeCodes(distinctCodes);
            // For each mapping found, add additional info to the item
            for (DegreeMapper mapping: mappings) {
                generateAdditionalMetadataFromMapper(ctx, item, currentItemService, mapping);
            }

            // 4) Update the item with the additional metadata
            currentItemService.update(ctx, item);

        } catch (SQLException e) {
            throw new GeneratorProcessException("An error occurred while replacing degree metadata", e);
        } catch (MalformedDegreeCodeException e) {
            throw new GeneratorProcessException("Could not generate degree code and label due to malformed degree code", e);
        } catch (AuthorizeException e) {
            throw new GeneratorProcessException("Not authorized to update the item", e);
        }
    }

    @Override
    public Boolean canBeProcessed(Context ctx, Item item) {
        String currentEntityType = item.getItemService().getEntityType(item);
        return currentEntityType != null && this.acceptedEntityTypes.contains(currentEntityType);
    }

    /**
     * Generate a list of code and label pairs from a list of metadata values.
     * @param metadataValues: List of metadata values of the ´degree.code´ field.
     * @return List of code and label pairs.
     * @throws MalformedDegreeCodeException
     */
    private List<List<String>> generateCodeAndLabels(List<MetadataValue> metadataValues) throws MalformedDegreeCodeException {
        List<List<String>> choppedList = new ArrayList<List<String>>();
        for (MetadataValue mv: metadataValues) {
            List<String> codeAndLabel = Arrays.asList(mv.getValue().split("-"));
            if (codeAndLabel.size() != 2) {
                throw new MalformedDegreeCodeException("Malformed degree code and label combination: " + mv.getValue());
            }
            codeAndLabel.replaceAll(String::trim);
            choppedList.add(codeAndLabel);
        }
        return choppedList;
    }

    /**
     * Clear previous metadata values for the degree code, degree label, root degree code, root degree label, faculty code and faculty name.
     * @param ctx: The DSpace context.
     * @param item: The item to update.
     * @param itemService: The item service used to make the operations.
     * @throws SQLException
     */
    private void clearPreviousMetadata(Context ctx, Item item, ItemService itemService) throws SQLException {
        itemService.clearMetadata(
            ctx, item,
            this.degreeLabelField.getSchema(),
            this.degreeLabelField.getElement(),
            this.degreeLabelField.getQualifier(),
            null
        );
        itemService.clearMetadata(
            ctx, item,
            this.rootDegreeCodeField.getSchema(),
            this.rootDegreeCodeField.getElement(),
            this.rootDegreeCodeField.getQualifier(),
            null
        );
        itemService.clearMetadata(ctx, item,
            this.rootDegreeLabelField.getSchema(),
            this.rootDegreeLabelField.getElement(),
            this.rootDegreeLabelField.getQualifier(),
            null
        );
        itemService.clearMetadata(
            ctx, item,
            this.facultyCodeField.getSchema(),
            this.facultyCodeField.getElement(),
            this.facultyCodeField.getQualifier(),
            null
        );
        itemService.clearMetadata(
            ctx, item,
            this.facultyNameField.getSchema(),
            this.facultyNameField.getElement(),
            this.facultyNameField.getQualifier(),
            null
        );
    }

    /**
     * Generate additional metadata from a `DegreeMapper` object and add it to the item.
     * @param ctx: The DSpace context.
     * @param item: The item to update.
     * @param itemService: The item service used to make the operations.
     * @param mapping: The degree mapper to use to generate the additional metadata.
     * @throws SQLException
     */
    private void generateAdditionalMetadataFromMapper(Context ctx, Item item, ItemService itemService, DegreeMapper mapping) throws SQLException {
        itemService.addMetadata(
            ctx, item,
            this.rootDegreeCodeField.getSchema(),
            this.rootDegreeCodeField.getElement(),
            this.rootDegreeCodeField.getQualifier(),
            null, mapping.getRootDegreeCode()
        );
        itemService.addMetadata(
            ctx, item,
            this.rootDegreeLabelField.getSchema(),
            this.rootDegreeLabelField.getElement(),
            this.rootDegreeLabelField.getQualifier(),
            null , mapping.getRootDegreeLabel()
        );
        itemService.addMetadata(
            ctx, item,
            this.facultyCodeField.getSchema(),
            this.facultyCodeField.getElement(),
            this.facultyCodeField.getQualifier(),
            null, mapping.getFacultyCode()
        );
        itemService.addMetadata(
            ctx, item,
            this.facultyNameField.getSchema(),
            this.facultyNameField.getElement(),
            this.facultyNameField.getQualifier(),
            null, mapping.getFacultyName()
        );
    }

    // -------------------
    // Getters && Setters
    // -------------------

    public List<String> getAcceptedEntityTypes(){
        return this.acceptedEntityTypes;
    }

    public void setAcceptedEntityTypes(List<String> acceptedEntityTypes){
        this.acceptedEntityTypes = acceptedEntityTypes;
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

    public MetadataField getRootDegreeCodeField(){
        return this.rootDegreeCodeField;
    }

    public void setRootDegreeCodeField(MetadataField rootDegreeCodeField){
        this.rootDegreeCodeField = rootDegreeCodeField;
    }

    public MetadataField getRootDegreeLabelField(){
        return this.rootDegreeLabelField;
    }

    public void setRootDegreeLabelField(MetadataField rootDegreeLabelField){
        this.rootDegreeLabelField = rootDegreeLabelField;
    }

    public MetadataField getFacultyCodeField(){
        return this.facultyCodeField;
    }

    public void setFacultyCodeField(MetadataField facultyCodeField){
        this.facultyCodeField = facultyCodeField;
    }

    public MetadataField getFacultyNameField(){
        return this.facultyNameField;
    }

    public void setFacultyNameField(MetadataField facultyNameField){
        this.facultyNameField = facultyNameField;
    }
}
