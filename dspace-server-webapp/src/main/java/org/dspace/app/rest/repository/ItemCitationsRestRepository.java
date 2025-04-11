/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ItemCitationsRest;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.citations.ItemCitation;
import org.dspace.uclouvain.citations.UCLouvainCitationsService;
import org.dspace.uclouvain.citations.UnknownCitationFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository to get a specific citation format for a given item.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@Component(ItemCitationsRest.CATEGORY + "." + ItemCitationsRest.PLURAL_NAME)
public class ItemCitationsRestRepository extends DSpaceRestRepository<ItemCitationsRest, UUID> {

    @Autowired
    ItemService itemService;

    @Autowired
    UCLouvainCitationsService uclouvainCitationsService;

    @Override
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public ItemCitationsRest findOne(Context context, UUID id) {
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        String format = request.getParameter("format");
        if (StringUtils.isEmpty(format)) {
            throw new BadRequestException("Missing format parameter");
        }

        try {
            Item item = itemService.find(context, id);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + id);
            }
            ItemCitationsRest citationsRest = new ItemCitationsRest();

            if (format.equals("all")) {
                // If 'all' is specified, generate a citation for each configured format.
                uclouvainCitationsService.getAllCitationsForItem(context, item).forEach(citation -> {
                    citationsRest.addCitation(citation.getFormat(), citation.getCitation());
                });
            } else {
                // If we have a valid specific format, generate the citation and add it to the rest object.
                ItemCitation citation = uclouvainCitationsService.getCitationForItem(context, item, format);
                if (citation != null) {
                    citationsRest.addCitation(citation.getFormat(), citation.getCitation());
                }
            }
            citationsRest.setId(id);
            return citationsRest;
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Could not find the related item to generate the citation.", e);
        } catch (UnknownCitationFormatException ucfe) {
            throw new BadRequestException("Unknown citation format.", ucfe);
        }
    }

    @Override
    public Page<ItemCitationsRest> findAll(Context context, Pageable pageable) {
        // Return unimplemented method exception since we cannot retrieve all the citations of all the items.
        throw new RepositoryMethodNotImplementedException(
            "No implementation found; Method not allowed!", ""
        );
    }

    @Override
    public Class<ItemCitationsRest> getDomainClass() {
        return ItemCitationsRest.class;
    }
}
