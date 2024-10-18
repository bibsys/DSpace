package org.dspace.uclouvain.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main controller to retrieve the affiliation tree structure.
 * The main structure can be sorted by parent UUID and depth.
 * - The parent UUID filter returns only the target affiliation and its children.
 * - The depth filter returns the children up to the given depth.
 *  Ex: (0 - would be only the target items and no children, 1 - would be the target items and their children, 2 - would be the target items, their children and their children's children, etc.).
 * 
 * @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@RestController
@RequestMapping("/api/uclouvain/affiliations")
public class AffiliationsRestController {

    private UCLouvainAffiliationEntityRestService uclouvainAffiliationEntityRestService;
    private Logger logger;

    public AffiliationsRestController(){
        this.uclouvainAffiliationEntityRestService = UCLouvainServiceFactory.getInstance().getAffiliationEntityRestService();
        this.logger = LogManager.getLogger(AffiliationsRestController.class);
    }

    /** 
     * Main endpoint to retrieve the affiliation tree structure.
     */ 
    @RequestMapping(value= "/affiliationStructure", method = RequestMethod.GET)
    public List<AffiliationEntityRestModel> getAffiliations(Context context, HttpServletResponse response, 
        @RequestParam(value = "parentUUID", required = false) UUID parentUUID,
        @RequestParam(value = "depth", required = false) Integer depth) throws IOException {

        // Get the tree from the service. This tree automatically regenerated once an OrgUnit is modified so we are sure to be up-to-date.
        List<AffiliationEntityRestModel> modelsList = this.uclouvainAffiliationEntityRestService.getAffiliationsEntities(context);

        // If nothing found return an empty list
        if (modelsList == null) {
            this.logger.warn("No affiliation tree found from 'UCLouvainAffiliationEntityRestService' service.");
            return new ArrayList<AffiliationEntityRestModel>();
        }

        List<AffiliationEntityRestModel> dataToReturn;

        // Parent filtering
        if (parentUUID != null) {
            AffiliationEntityRestModel searchedElement = modelsList.stream().filter(model -> model.UUID.equals(parentUUID)).findFirst().orElse(null);
            if (searchedElement == null) {
                response.sendError(404, "Given affiliation UUID not valid or not found");
                return null;
            }
            dataToReturn =  new ArrayList<AffiliationEntityRestModel>(Arrays.asList(searchedElement));
        } else {
            dataToReturn = modelsList.stream().filter(model -> model.parent == null).collect(Collectors.toList());
        }

        // Depth filtering
        if (depth != null && depth < dataToReturn.size()) {
            if (depth < 0) {
                response.sendError(400, "Depth parameter invalid, it should be a positive integer");
                return null;
            }
            recursiveDepthRemover(dataToReturn, depth, 0);
        }
        return dataToReturn;
    }

    /**
     * Recursive method to filter a list of AffiliationEntityRestModel to the desired depth.
     * 
     * @param models: The list of AffiliationEntityRestModel to filter.
     * @param maxDepth: The maximum depth to keep.
     * @param currentDepth: The current depth of the recursion.
     */
    private void recursiveDepthRemover(List<AffiliationEntityRestModel> models, Integer maxDepth, Integer currentDepth) {
        if (maxDepth == currentDepth) {
            // Empty the children since we reached the desired depth.
            models.forEach(model -> model.children = new ArrayList<AffiliationEntityRestModel>());
        } else {
            // We have not yet reached the desired depth, so we continue the recursion.
            models.forEach(model -> recursiveDepthRemover(model.children, maxDepth, currentDepth + 1));
        }
    }
}
