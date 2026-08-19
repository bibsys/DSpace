/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.ItemCitationsRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.citations.CitationEntry;
import org.dspace.uclouvain.citations.ItemCitations;
import org.dspace.uclouvain.citations.UCLouvainCitationsService;
import org.dspace.uclouvain.citations.UnknownCitationFormatException;
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
    UCLouvainCitationsService citationsService;

    @PreAuthorize("permitAll()")
    public ItemCitationsRest getAllCitations(
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
            List<String> crosswalks = citationsService.getAvailableCitationsCrosswalks(context, item);
            List<CitationEntry> citations = generateCitations(context, item, crosswalks);
            return converter.toRest(new ItemCitations(itemId, citations), utils.obtainProjection());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a citations for a list of crosswalk
     * @param context the DSpace application context
     * @param item the item to generate citation
     * @param crosswalks the list of crosswalk to use
     * @return the list of citation corresponding to desired crosswalks
     * @throws UnknownCitationFormatException If a crosswalk is unknown
     */
    private List<CitationEntry> generateCitations(Context context, Item item, List<String> crosswalks)
            throws UnknownCitationFormatException {
        List<CitationEntry> citations = new ArrayList<>();
        for (String crosswalk : crosswalks) {
            String citation = citationsService.getCitationForItemByCrosswalk(context, item, crosswalk);
            if (citation != null && !citation.isBlank()) {
                citations.add(new CitationEntry(crosswalk, citation));
            }
        }
        return citations;
    }
}
