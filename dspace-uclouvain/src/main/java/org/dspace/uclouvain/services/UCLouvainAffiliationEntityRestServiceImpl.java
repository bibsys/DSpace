package org.dspace.uclouvain.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;

/**
 * Service to generate an affiliation entity tree.
 * It calls Solr to get all the OrgUnits and convert them into a list of 'AffiliationsEntities'.
 * 
 * @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainAffiliationEntityRestServiceImpl implements UCLouvainAffiliationEntityRestService {
    private SearchService searchService;
    private Logger logger;

    public UCLouvainAffiliationEntityRestServiceImpl() {
        this.searchService = SearchUtils.getSearchService();
        this.logger = LogManager.getLogger(UCLouvainAffiliationEntityRestServiceImpl.class);
    }

    public List<AffiliationEntityRestModel> getAffiliationsEntities(Context context) {
        try {
            // Retrieve all OrgUnits from Solr
            SolrDocumentList orgUnits = this.getOrgUnitsSolrDocument(context);
            if (orgUnits == null) {
                return null;
            }

            // Convert the list of OrgUnits to a list of AffiliationEntityRestModel 
            List<AffiliationEntityRestModel> modelsList = this.getModelsFromDocuments(orgUnits);
            
            // Generate the tree structure of the affiliations
            this.createAffiliationsStructure(modelsList);
            return modelsList;
        } catch (Exception e) {
            this.logger.error("Error while processing affiliations entities: " + e);
            return null;
        }
    }

    /**
     * Creates the tree structure of the affiliations entities using the 'parent' property of the AffiliationEntityRestModel to find the UUID of the parent.
     * @param models: The list of AffiliationEntityRestModel to create the tree structure from.
     */
    private void createAffiliationsStructure(List<AffiliationEntityRestModel> models) {
        for (AffiliationEntityRestModel model: models) {
            if (model.parent != null) {
                AffiliationEntityRestModel parent = models.stream().filter(m -> m.UUID.equals(model.parent)).findFirst().orElse(null);
                if (parent != null) {
                    parent.children.add(model);
                }
            }
        }
    }

    /**
     * Converts a list of SolrDocuments into a list of AffiliationEntityRestModel.
     * Maps the metadata of the SolrDocument to the properties of the model.
     * @param documents: The list of SolrDocuments to convert.
     * @return A list of AffiliationEntityRestModel.
     */
    private List<AffiliationEntityRestModel> getModelsFromDocuments(SolrDocumentList documents) {
        List<AffiliationEntityRestModel> models = new ArrayList<AffiliationEntityRestModel>();
        for (SolrDocument document: documents) {
            AffiliationEntityRestModel model = new AffiliationEntityRestModel();

            String itemID = documentExtract(document, "search.resourceid");
            if (itemID != null) {
                model.UUID = UUID.fromString(itemID);
                model.name = documentExtract(document, "title", "");
                model.acronym = documentExtract(document, "oairecerif.acronym", "");
                model.type = documentExtract(document, "dc.type", "");

                String selectable = documentExtract(document, "organization.isSelectable");
                model.isSelectable = (selectable != null) ? selectable.equals("true"): true;

                // Extract parent UUID
                String parentAuthority = documentExtract(document, "organization.parentOrganization_authority");
                try {
                    if (parentAuthority != null) {
                        model.parent = UUID.fromString(parentAuthority);
                    }
                } catch (Exception e) {
                    // LOG error, authority should be a valid UUID string
                    this.logger.error("Error while parsing parent UUID of OrgUnit with ID: " + itemID + " - error: " + e);
                }
                model.children = new ArrayList<AffiliationEntityRestModel>();
                models.add(model);
            } else {
                // Log as a warning that an item has no UUID and add a list of all his metadata for investigation purpose.
                Map<String, Collection<Object>> valuesMap =  document.getFieldValuesMap();
                String documentContent = "";
                for (String key: valuesMap.keySet()){
                    String keyValues = key + "[" + valuesMap.get(key).stream().map(value -> value.toString()).collect(Collectors.joining(", ")) + "]";
                    documentContent = documentContent + keyValues;
                }
                this.logger.warn("Could not find UUID for SolrDocument with following metadata:\n" + documentContent);
            }
        }
        return models;
    }

    /**
     * Little util method to extract the first value of a SOLR document field.
     * If nothing is found for a field, return the null. Else return the value as a string.
     * 
     * @param document: The SOLR document ot search values in.
     * @param field: The field to search for.
     * @return: The first value in the document for the given field or null if not found.
     */
    private String documentExtract(SolrDocument document, String field) {
        return this.documentExtract(document, field, null);
    }

    /**
     * Little util method to extract the first value of a SOLR document field.
     * If nothing is found for a field, return the default value. Else return the value as a string.
     * 
     * @param document: The SOLR document ot search values in.
     * @param field: The field to search for.
     * @param defaultValue: The value to return if the field value is null.
     * @return: The first value in the document for the given field or default value if not found.
     */
    private String documentExtract(SolrDocument document, String field, String defaultValue) {
        Object value = document.getFirstValue(field);
        return (value != null) ? value.toString() : defaultValue;
    }

    /**
     * Retrieve all OrgUnits (affiliations entities) from Solr.
     * @param context: The current DSpace context.
     * @return A list of SolrDocuments representing the OrgUnits (Affiliations).
     * @throws SolrServerException
     * @throws IOException
     */
    private SolrDocumentList getOrgUnitsSolrDocument(Context context) throws SolrServerException, IOException {
        SolrClient client = this.searchService.getSolrSearchCore().getSolr();
        if (client == null) {
            throw new SolrServerException("Could not load the SolrClient to search for OrgUnits.");
        }
        QueryResponse response = client.query(getOrgUnitSolrQuery());
        return response.getResults();
    }

    /**
     * Returns the query to get all OrgUnits from Solr.
     * @return The Solr query.
     */
    private SolrQuery getOrgUnitSolrQuery() {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("search.entitytype:\"OrgUnit\" && search.resourcetype: \"Item\" && discoverable:\"true\"");
        solrQuery.setRows(1000);
        return solrQuery;
    }
}
