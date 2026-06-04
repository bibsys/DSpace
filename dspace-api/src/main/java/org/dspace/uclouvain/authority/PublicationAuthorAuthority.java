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
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.profile.ResearcherProfile;

/**
 * Simple authority to search for Persons.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class PublicationAuthorAuthority extends PublicationAuthority {

    protected static final String DATA_PREFIX = "data-";
    protected static final String AUTHORITY_PREFIX = "authority-";


    // CLASS ATTRIBUTES ================================================================================================
    protected String authorityName;
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    // IMPLEMENTED ABSTRACT METHODS ====================================================================================
    /** The filter query that will give us only Persons item in the search results. */
    @Override
    protected String getEntityTypeFilterString() {
        return "dspace.entity.type:Person";
    }

    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }

    /** Generate extra information to fill some fields in the forms. */
    @Override
    protected Map<String, String> generateExtras(Item item) {
        Map<String, String> extras = new HashMap<>();
        fillMetadata(
            extras,
            item,
            ResearcherProfile.OFFICIAL_EMAIL_FIELD,
            "authors_email",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.ORCID_FIELD,
            "authors_identifier_orcid",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.FGS_FIELD,
            "authors_identifier_fgs",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.INSTITUTION_FIELD,
            "authors_institution_code",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.DEPARTMENT_FIELD,
            "authors_entity_name",
            false
        );
        return extras;
    }

    /**
     * Get main label for the authority
     * @param key the UUID of the authority
     * @param locale the local language to translate the found value
     * @return the localized main label to use for this authority.
     */
    @Override
    public String getLabel(String key, String locale) {
        try {
            Item person = itemService.find(getContext(), UUID.fromString(key));
            return Optional.ofNullable(person)
                .map(item -> itemService.getMetadataFirstValue(item, "dc", "title", null, null))
                .filter(name -> !name.isBlank())
                .orElse(key);
        } catch (IllegalArgumentException | SQLException e) {
            return key;
        }
    }

    // PRIVATE METHODS =================================================================================================
    /**
     * Fill the extras map with additional value (and authority)
     *   If the desired metadata isn't found into the item, the map will fill with an empty string.
     *   Using this trick, the frontend form could be empty from a potential previous encoded value
     * @param extras the map to fill if any value is found
     * @param item the item to search for
     * @param mdString the configuration key containing the mdString where to search the metadata value
     * @param mapKey the map key in which to store the metadata value (if any metadata value is found)
     * @param authorityLinked if this key must be linked to the item by authority
     */
    protected void fillMetadata(
        Map<String, String> extras,
        Item item,
        String mdString,
        String mapKey,
        boolean authorityLinked
    ) {
        if (mdString == null) {
            return;
        }
        String value = itemService.getMetadataByMetadataString(item, mdString)
            .stream()
            .findFirst()
            .map(MetadataValue::getValue)
            .orElse("");
        extras.put(DATA_PREFIX + mapKey, value);
        if (StringUtils.isNotBlank(value) && authorityLinked) {
            extras.put(AUTHORITY_PREFIX + mapKey, item.getID().toString());
        }
    }

    public String getLinkedEntityType() {
        return ResearcherProfile.ENTITY_TYPE;
    }
}
