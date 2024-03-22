package org.dspace.uclouvain.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceSearchPlugin;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Solr plugin used to add a or many filter(s) on the degree code of the workflow items.
 * First, we extract them from the current eperson's metadata.
 * After that we add them to the query filters.
 * If a user has no degree code, nothing should be returned from Solr. 
 */
public class SolrServiceWorkflowMetadataRestrictionsPlugin implements SolrServiceSearchPlugin {

    @Autowired
    ItemService itemService;

    @Autowired
    ResearcherProfileService researcherProfileService;

    private String degreeMetadataFilterFieldName = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("uclouvain.solr.plugin.workflow.degree.field.filter", "degreecode_keyword");
    private String degreeMetadataFieldName = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("uclouvain.solr.plugin.workflow.degree.field.metadata", "crisrp.workgroup");

    /**
     * The name of the discover configuration used to search for workflow tasks in the mydspace
     */
    public static final String DISCOVER_WORKFLOW_CONFIGURATION_NAME = "workflow";


    @Override
    public void additionalSearchParameters(
            Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery
    ) throws SearchServiceException {
        try {
            boolean isWorkflow = StringUtils.startsWith(
                discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKFLOW_CONFIGURATION_NAME
            );

            EPerson currentUser = context.getCurrentUser();
            if (currentUser != null && isWorkflow) {          
                // Retrieve the current user's researcher profile that can contain metadata about the degree codes
                ResearcherProfile currentProfile = researcherProfileService.findById(context, currentUser.getID());
                StringBuilder controllerQuery = new StringBuilder();

                List<MetadataValue> degreeCodes = (currentProfile != null) ? itemService.getMetadataByMetadataString(currentProfile.getItem(), degreeMetadataFieldName) : new ArrayList<>();
                // If the profile has no degree codes, just return nothing in the solr search
                if (degreeCodes == null || degreeCodes.isEmpty()) {
                    controllerQuery.append("dc.title:(\"\")");
                }
                // Else add a filter argument for each code
                else {
                    String degreeQuery = String.join(" OR ", degreeCodes.stream().map(x -> x.getValue().trim()).collect(Collectors.toList()));
                    controllerQuery.append(this.degreeMetadataFilterFieldName + ":(" + degreeQuery + ")");
                }
                    
                solrQuery.addFilterQuery(controllerQuery.toString());
            }
        } catch (SQLException e) {
            throw new SearchServiceException("SQL error occurred while searching for the profile", e);
        } catch (AuthorizeException e) {
            throw new SearchServiceException("Not authorized to access a resource", e);
        } catch (Exception e){
            throw new SearchServiceException(e);
        }
    }
}
