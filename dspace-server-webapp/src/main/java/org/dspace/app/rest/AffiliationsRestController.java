/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main controller to retrieve the affiliation tree structure.
 * The main structure can be sorted by parent UUID and depth.
 * - The parent UUID filter returns only the target affiliation and its children.
 * - The depth filter returns the children up to the given depth.
 * Ex:
 *  0 - would be only the target items and no children,
 *  1 - would be the target items and their children,
 *  2 - would be the target items, their children and their children's children, etc.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@Validated
@RestController
@RequestMapping("/api/uclouvain/affiliations")
public class AffiliationsRestController {

    private final UCLouvainAffiliationEntityRestService affiliationEntityRestService;

    public AffiliationsRestController() {
        this.affiliationEntityRestService = UCLouvainServiceFactory
            .getInstance()
            .getAffiliationEntityRestService();
    }

    /** 
     * Main endpoint to retrieve the affiliation tree structure.
     */
    @RequestMapping(value = "/affiliationStructure", method = RequestMethod.GET)
    public List<AffiliationEntityRestModel> getAffiliations(Context context, HttpServletResponse response,
        @RequestParam(value = "parentUUID", required = false) UUID parentUUID,
        @RequestParam(value = "depth", required = false) @Min(0) Integer depth,
        @RequestParam(value = "documentCount", required = false, defaultValue = "false") boolean docCount
    ) {
        int depthValue = (depth == null) ? Integer.MAX_VALUE : depth;
        return affiliationEntityRestService.getAffiliationsEntities(context, parentUUID, depthValue, docCount);
    }
}
