/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ItemCitationsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.uclouvain.citations.ItemCitations;
import org.springframework.stereotype.Component;

@Component
public class ItemCitationsConverter implements DSpaceConverter<ItemCitations, ItemCitationsRest> {
    @Override
    public ItemCitationsRest convert(ItemCitations citations, Projection projection) {
        ItemCitationsRest restResponse = new ItemCitationsRest();
        citations.getCitations().forEach(citation -> {
            restResponse.addCitation(citation.getFormat(), citation.getCitation());
        });
        return restResponse;
    }

    @Override
    public Class<ItemCitations> getModelClass() {
        return ItemCitations.class;
    }
}