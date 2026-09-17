/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.junit.Test;

/**
 * {@link ItemService#setMetadataInPlace}: a value written at a place that does not exist yet must land at that
 * place with the given confidence, after the existing values.
 */
public class ItemServiceSetMetadataInPlaceIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Test
    public void newPlaceIsAppendedWithItsConfidence() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).withName("C").build();
        Item item = ItemBuilder.createItem(context, collection).withTitle("T").withAuthor("First, Author").build();

        itemService.setMetadataInPlace(context, item, "dc.contributor.author", null, "Second, Author", null, 1,
            Choices.CF_UNSET);
        itemService.setMetadataInPlace(context, item, "dc.contributor.author", null, "Third, Author", null, 2,
            Choices.CF_ACCEPTED);
        itemService.update(context, item);
        context.commit();

        List<MetadataValue> authors = itemService.getMetadataByMetadataString(item, "dc.contributor.author");
        assertEquals(List.of("First, Author", "Second, Author", "Third, Author"),
            authors.stream().map(MetadataValue::getValue).toList());
        assertEquals(List.of(0, 1, 2), authors.stream().map(MetadataValue::getPlace).toList());
        assertEquals(Choices.CF_ACCEPTED, authors.get(2).getConfidence());
    }
}
