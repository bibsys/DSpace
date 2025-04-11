/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ItemCitationsRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.citations.UCLouvainCitationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository to get the all the citation of an item.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.PLURAL_NAME + "." + ItemRest.CITATIONS)
public class ItemCitationsLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    ItemService itemService;

    @Autowired
    UCLouvainCitationsService uclouvainCitationsService;

    @PreAuthorize("permitAll()")
    public ItemCitationsRest getCitations(
        @Nullable HttpServletRequest request,
        UUID itemId,
        @Nullable Pageable optionalPageable,
        Projection projection
    ) {
        try {
            // 1. Get the item using the itemId.
            // 2. Extract Citations for the item using the citations service.
            // 3. Build the rest object and return it.
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }

            ItemCitationsRest citationsRest = new ItemCitationsRest();
            uclouvainCitationsService.getAllCitationsForItem(context, item).forEach(citation -> {
                citationsRest.addCitation(citation.getFormat(), citation.getCitation());
            });

            return citationsRest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
