/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ItemCitationsRest;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.citations.CitationEntry;
import org.dspace.uclouvain.citations.ItemCitations;
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
    UCLouvainCitationsService citationsService;

    @Override
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public ItemCitationsRest findOne(Context context, UUID id) {
        // To render citation(s), some argument must be provided into URL query parameter:
        //   * Either a "crosswalk" argument: In this case, this is this crosswalk that will be used to generate the
        //     citation. Using this argument could only return 1 citation max.
        //   * Either a "style" argument: In this case, the entityType of the item should be used to determine which
        //     crosswalk to used.
        //   * (optional) "format" argument: In combination with "style" argument, to return a specific output format.
        //     by default, if no format is specific, the return should be a text/plain string.

        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        String crosswalkParam = request.getParameter("crosswalk");
        String styleParam = request.getParameter("style");
        String formatParam = request.getParameter("format");
        if (StringUtils.isEmpty(crosswalkParam) && StringUtils.isEmpty(styleParam)) {
            throw new DSpaceBadRequestException("Mising required 'crosswalk' or 'style' parameter");
        }
        Item item = loadItem(context, id);
        List<CitationEntry> citations = (StringUtils.isNotBlank(crosswalkParam))
            ? getCitationsByCrosswalk(context, item, crosswalkParam)
            : getCitationBySimpleFormat(context, item, styleParam, formatParam);
        return converter.toRest(new ItemCitations(id, citations), utils.obtainProjection());
    }

    @Override
    public Page<ItemCitationsRest> findAll(Context context, Pageable pageable) {
        // Return unimplemented method exception since we cannot retrieve all the citations of all the items.
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Class<ItemCitationsRest> getDomainClass() {
        return ItemCitationsRest.class;
    }

    // PRIVATE METHODS =================================================================================================
    private Item loadItem(Context context, UUID id) throws RuntimeException {
        try {
            Item item = itemService.find(context, id);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + id);
            }
            return item;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<CitationEntry> getCitationsByCrosswalk(Context context, Item item, String crosswalk)
        throws UnknownCitationFormatException {
        String citation = citationsService.getCitationForItemByCrosswalk(context, item, crosswalk);
        return StringUtils.isNotBlank(citation)
            ? List.of(new CitationEntry(crosswalk, citation))
            : Collections.emptyList();
    }

    private List<CitationEntry> getCitationBySimpleFormat(Context context, Item item, String style, String format)
        throws UnknownCitationFormatException {
        style = (Objects.equals(style, "*")) ? UCLouvainCitationsService.ALL_STYLE : style;
        format = (Objects.equals(format, "*")) ? UCLouvainCitationsService.ALL_FORMAT : format;
        return citationsService.getCitationForItem(context, item, style, format)
            .entrySet().stream()
            .map((entry) -> new CitationEntry(entry.getKey(), entry.getValue()))
            .toList();
    }
}
