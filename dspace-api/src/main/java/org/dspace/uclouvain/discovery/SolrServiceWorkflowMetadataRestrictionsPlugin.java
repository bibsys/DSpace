/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
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
 * Solr plugin used to add one or many filter(s) on the degree code of the workflow items.
 *  - First, we extract them from the current eperson's metadata,
 *  - Next, we add them to the query filters.
 *  - If a user has no degree code, nothing should be returned from Solr.
 */
public class SolrServiceWorkflowMetadataRestrictionsPlugin implements SolrServiceSearchPlugin {

    @Autowired
    ItemService itemService;
    @Autowired
    ResearcherProfileService researcherProfileService;
    @Autowired
    AuthorizeService authorizeService;

    private String degreeMetadataFilterFieldName = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getProperty("uclouvain.solr.plugin.workflow.degree.field.filter", "degreecode_keyword");
    private String degreeMetadataFieldName = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getProperty("uclouvain.solr.plugin.workflow.degree.field.metadata", "crisrp.workgroup");

    /**
     * The name of the 'discover configuration' used to search for workflow tasks in the myDspace
     */
    public static final String DISCOVER_WORKFLOW_CONFIGURATION_NAME = "workflow";


    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery)
            throws SearchServiceException {

        // skip all queries except for workflow (aka validation) query
        // If user isn't connected (anonymous), no restriction can be created
        EPerson currentUser = context.getCurrentUser();
        boolean isWorkflow = StringUtils.startsWith(
            discoveryQuery.getDiscoveryConfigurationName(),
            DISCOVER_WORKFLOW_CONFIGURATION_NAME
        );
        if (!isWorkflow || currentUser == null) {
            return;
        }

        try {
            // If the current-logged user is an administrator, don't create any restriction.
            // Admin can manage any workflow item
            if (authorizeService.isAdmin(context)) {
                return;
            }

            // Retrieve the profile related to the current-logged user.
            // This profile contains metadata about the degree codes that user is manager for
            ResearcherProfile currentProfile = researcherProfileService.findById(context, currentUser.getID());
            List<MetadataValue> degreeCodes = (currentProfile != null)
                ? itemService.getMetadataByMetadataString(currentProfile.getItem(), degreeMetadataFieldName)
                : new ArrayList<>();

            // If the profile doesn't contain any degree codes, force Solr to return empty response
            if (degreeCodes == null || degreeCodes.isEmpty()) {
                solrQuery.addFilterQuery("-*:*");  // `-*:*` --> exclude result with a defined field
                return;
            }

            String degreeQuery = degreeCodes
                .stream()
                .map(x -> x.getValue().trim())
                .collect(Collectors.joining(" OR ")
            );
            String fqTerm = this.degreeMetadataFilterFieldName + ":(" + degreeQuery + ")";
            solrQuery.addFilterQuery(fqTerm);

        } catch (SQLException e) {
            throw new SearchServiceException("SQL error occurred while searching for the profile", e);
        } catch (AuthorizeException e) {
            throw new SearchServiceException("Not authorized to access a resource", e);
        } catch (Exception e) {
            throw new SearchServiceException(e);
        }
    }
}
