/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uclouvain.authorize.item.PublicationItemAuthorize;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Dispatch of item authorization by entity type.
 */
@RunWith(MockitoJUnitRunner.class)
public class UCLouvainAuthorizeServiceImplTest {

    @Mock
    private ItemService itemService;
    @Mock
    private PublicationItemAuthorize publicationItemAuthorize;
    @Mock
    private Context context;
    @Mock
    private Item item;
    @InjectMocks
    private UCLouvainAuthorizeServiceImpl service;

    @Test
    public void itemWithoutEntityTypeIsNotAuthorized() {
        when(itemService.getEntityType(item)).thenReturn(null);

        assertFalse(service.authorizeActionBoolean(context, item, Constants.READ, null));
        verifyNoInteractions(publicationItemAuthorize);
    }

    @Test
    public void publicationIsDelegatedToPublicationRules() {
        when(itemService.getEntityType(item)).thenReturn(Publication.ENTITY_TYPE);
        when(publicationItemAuthorize.authorizeActionBoolean(context, item, Constants.READ, null)).thenReturn(true);

        assertTrue(service.authorizeActionBoolean(context, item, Constants.READ, null));
    }
}
