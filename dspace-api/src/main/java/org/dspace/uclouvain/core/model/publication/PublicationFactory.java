/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Factory class used to retrieve a Publication object from a classic Item.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PublicationFactory {

    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static final ConfigurationService configService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private PublicationFactory() {}

    public static Publication build(Item item) throws InvalidModelEntityTypeException {
        String docType = itemService.getMetadataFirstValue(
            item,
            new MetadataFieldName(Publication.MAIN_TYPE_FIELD),
            null
        );
        switch (docType) {
            case SpeechPublication.DOCUMENT_TYPE:
                return new SpeechPublication(item);
            case ArticlePublication.DOCUMENT_TYPE:
                return new ArticlePublication(item);
            case DissertationPublication.DOCUMENT_TYPE:
                return new DissertationPublication(item);
            // TODO :: Add other specific publication type
            default:
                return new Publication(item);
        }
    }
}
