/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Optional;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Item in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ItemConverter
        extends DSpaceObjectConverter<Item, ItemRest>
        implements IndexableObjectConverter<Item, ItemRest> {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataSecurityService metadataSecurityService;

    @Autowired
    private ConfigurationService configService;

    @Override
    public ItemRest convert(Item obj, Projection projection) {
        ItemRest item = super.convert(obj, projection);
        item.setChangeRequested(this.isChangeRequested(obj));
        item.setInArchive(obj.isArchived());
        item.setDiscoverable(obj.isDiscoverable());
        item.setWithdrawn(obj.isWithdrawn());
        item.setLastModified(obj.getLastModified());
        item.setEntityType(itemService.getEntityTypeLabel(obj));
        this.enforceDspaceEntityMetadata(item);
        return item;
    }

    /**
     * Retrieves the metadata list filtered according to the hidden metadata configuration
     * When the context is null, it will return the metadata list as for an anonymous user
     * Overrides the parent method to include virtual metadata
     * @param context    The context
     * @param item       The object of which the filtered metadata will be retrieved
     * @param projection The projection(s) used into current request
     * @return A list of object metadata (including virtual metadata) filtered based on the hidden metadata
     *         configuration
     */
    @Override
    public MetadataValueList getPermissionFilteredMetadata(Context context, Item item, Projection projection) {
        boolean preventSecurityCheck = preventSecurityCheck(projection);
        return (projection.isAllLanguages())
            ? new MetadataValueList(metadataSecurityService.getPermissionFilteredMetadataValues(context, item, preventSecurityCheck))
            : new MetadataValueList(metadataSecurityService.getPermissionAndLangFilteredMetadataFields(context, item, preventSecurityCheck));
    }

    public boolean checkMetadataFieldVisibility(Context context, Item item, MetadataField metadataField) {
        return metadataSecurityService.checkMetadataFieldVisibility(context, item, metadataField);
    }

    private boolean preventSecurityCheck(Projection projection) {
        return Optional.ofNullable(projection)
            .map(Projection::preventMetadataLevelSecurity)
            .orElse(false);
    }

    @Override
    protected ItemRest newInstance() {
        return new ItemRest();
    }

    @Override
    public Class<Item> getModelClass() {
        return Item.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Item;
    }

    /**
     * Checks if the item requires changes by looking at a specific metadata.
     * @param item The item to analyze
     * @return True if any active changes are requested for this item; False otherwise
     */
    private boolean isChangeRequested(Item item) {
        String activeChangeRequestedFieldName = this.configService.getProperty("uclouvain.global.metadata.activerequestchange.field");
        MetadataFieldName metadataField = new MetadataFieldName(activeChangeRequestedFieldName);
        return item
                .getMetadata()
                .stream()
                .anyMatch(mv -> mv.getMetadataField().toString('.').equals(metadataField.toString()));
    }

    /**
     * Enforce item rest response to have `dspace.entity.type` metadata if this metadata isn't yet present
     * into database stored metadata; This could happen for Workspace or Workflow item...
     * @param rest The rest representation of an item.
     */
    private void enforceDspaceEntityMetadata(ItemRest rest) {
        MetadataFieldName dspaceEntityTypeField = new MetadataFieldName("dspace", "entity", "type");
        if (!rest.getMetadata().getMap().containsKey(dspaceEntityTypeField.toString())) {
            MetadataRest<MetadataValueRest> metadata = rest.getMetadata();
            metadata.put(dspaceEntityTypeField.toString(), new MetadataValueRest(rest.getEntityType()));
            rest.setMetadata(metadata);
        }
    }
}
