/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    protected String getEntityTypeFilterString() {
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
            extras.put("authority-authors_email", item.getID().toString());
        }
        if (orcid != null) {
            extras.put("data-authors_identifier_orcid", orcid);
            extras.put("authority-authors_identifier_orcid", item.getID().toString());
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
}
