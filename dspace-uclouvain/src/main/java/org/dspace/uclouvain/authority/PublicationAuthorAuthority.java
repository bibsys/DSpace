package org.dspace.uclouvain.authority;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Item;

/**
 * Simple authority to search for Persons.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class PublicationAuthorAuthority extends PublicationAuthority {
    private String authorityName;

    /**
     * The filter query that will give us only Persons item in the search results.
     */
    @Override
    protected String getEntityTypeFilterString(){
        return "dspace.entity.type:Person";
    }

    /**
     * Generate extra information to fill some fields in the forms.
     */
    @Override
    protected Map<String, String> generateExtras(Item item) throws SQLException {
        Map<String, String> extras = new HashMap<String, String>();
        String email = this.itemService.getMetadataFirstValue(item, "person", "email", null, null);
        String orcid = this.itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", null);
        String orgUnit = this.itemService.getMetadataFirstValue(item, "person", "affiliation", "name", null);
        if (email != null) {
            extras.put("data-authors_email", email);
        }
        if (orcid != null) {
            extras.put("data-authors_identifier_orcid", orcid);
        }
        if (orgUnit != null) {
            extras.put("data-oairecerif_authors_orgunit", orgUnit);
        }
        // TODO: Find how/where to extract institution name && affiliation departement
        extras.put("data-authors_institution_name", "Université Catholique de Louvain");
        extras.put("data-oairecerif_authors_orgunitDepartement", "Test departement");
        return extras;
    }

    @Override
    public String getLabel(String key, String locale) {
        return "";
    }

    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
