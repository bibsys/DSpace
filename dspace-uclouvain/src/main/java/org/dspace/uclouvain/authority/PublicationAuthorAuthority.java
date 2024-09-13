package org.dspace.uclouvain.authority;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

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
        if (email != null) {
            extras.put("data-authors_email", email);
        }
        if (orcid != null) {
            extras.put("data-authors_identifier_orcid", orcid);
        }

        List<MetadataValue> institutions = this.itemService.getMetadata(item, "person", "affiliation", "institution", null);
        List<MetadataValue> departments = this.itemService.getMetadata(item, "person", "affiliation", "department", null);

        // For institutions && department, we must return something only if there is only one value, else we cannot choose.
        MetadataValue institution = (institutions.isEmpty() || institutions.size() != 1) ? null : institutions.get(0);
        if (institution != null) {
            extras.put("data-oairecerif_authors_orgunit", institution.getValue());
            if (isValidAuthority(institution.getAuthority())) {
                extras.put("authority-oairecerif_authors_orgunit", institution.getAuthority());
            }
        }

        MetadataValue department = (departments.isEmpty() || departments.size() != 1) ? null : departments.get(0);
        if (department != null) {
            extras.put("data-oairecerif_authors_orgunitDepartement", department.getValue());
            if (isValidAuthority(department.getAuthority())) {
                extras.put("authority-oairecerif_authors_orgunitDepartement", department.getAuthority());
            }
        }
        return extras;
    }

    @Override
    public String getLabel(String key, String locale) {
        try {
            Item person = this.itemService.find(getContext(), UUID.fromString(key));
            if (person != null) {
                String name =  this.itemService.getMetadataFirstValue(person, "dc", "title", null, null);
                if (name != null) {
                    return name;
                }
            }
            return key;
        } catch (SQLException e) {
            return key;
        }

    }

    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }

    private boolean isValidAuthority(String authority) {
        try {
            UUID.fromString(authority);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
