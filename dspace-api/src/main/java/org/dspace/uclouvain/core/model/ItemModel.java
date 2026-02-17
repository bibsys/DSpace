/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.web.ContextUtil;
import org.springframework.util.Assert;

/**
 * Abstract skeleton class to represent internal specific Item
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class ItemModel {

    // METADATA FIELDS DEFINITIONS =====================================================================================
    protected static final String FIELD_PREFIX = "uclouvain.global.metadata.";

    // CLASS ATTRIBUTES ================================================================================================
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static final ConfigurationService configService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    protected Item item;
    protected Context context;

    // CONSTRUCTOR =====================================================================================================
    protected ItemModel(Item item) {
        Assert.notNull(item, "An ItemModel requires an item");
        this.item = item;
        try {
            Context context = ContextUtil.obtainCurrentRequestContext();
            this.context = (context != null) ? context : new Context();
        } catch (Exception e) {
            this.context = new Context();
        }
    }

    // GETTER ==========================================================================================================
    public Item getItem() {
        return this.item;
    }

    public String getTitle() {
        return getFirstMetadataValue("dc.title");
    }

    public UUID getID() {
        return this.item.getID();
    }

    // FUNCTIONS =======================================================================================================
    /**
     * Get a list of metadata values corresponding to the metadata field.
     * @param mdField the metadata field name with '.' as separator.
     * @return the list of corresponding values (returning at least an empty list)
     */
    protected List<String> getMetadataValues(String mdField) {
        return getMetadata(mdField)
            .stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Get the first found metadata value corresponding to the metadata field.
     * @param mdField the metadata field name with '.' as separator.
     * @return the corresponding metadata value if exists; null otherwise
     */
    protected String getFirstMetadataValue(String mdField) {
        return getMetadataValues(mdField).stream().findFirst().orElse(null);
    }

    private List<MetadataValue> getMetadata(String mdField) {
        return itemService.getMetadataByMetadataString(item, mdField);
    }
}
