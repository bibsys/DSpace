/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.Journal;

/**
 * Simple authority to search for journal objects in DSpace.
 * The main search field is the title of the object (name of the journal).
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PublicationJournalAuthority extends PublicationAuthority {
    private String authorityName;

    /**
     * The filter query that will give us only Journal items in the search results.
     */
    @Override
    protected String getEntityTypeFilterString() {
        return "dspace.entity.type:Journal";
    }

    /**
     * Generate extra information that will be used to complete the form.
     * In this case we add the issn of the journal and try to add a potential 'peer-reviewed' status.
     * Note the 'authority' key that will allow the future metadata value to be linked to the Journal object.
     * 
     * @param item The item to generate extra information for.
     * @throws Exception
     */
    @Override
    protected Map<String, String> generateExtras(Item item) throws Exception {
        Map<String, String> extras =  new HashMap<>();
        Journal journal = new Journal(item);
        String issn = journal.getIdentifier(Journal.ISSN_IDENTIFIER);
        String eissn = journal.getIdentifier(Journal.EISSN_IDENTIFIER);
        String publisher = journal.getPublisher();
        String publisherLocation = journal.getPublisherLocation();
        String peerReviewed = String.valueOf(journal.isPeerReviewed());
        String statusCode = journal.getStatusCode();

        String authority = item.getID().toString();

        if (isNotEmpty(statusCode) && statusCode.equals(Journal.JOURNAL_CEASED_ACCESS_TYPE)) {
            extras.put("journal.searchresult.ceased", statusCode);
        }

        // First put the info string for this specific search result entry.
        String resultDetails = getFullDetailString(issn, publisher, publisherLocation);
        extras.put("journal.searchresult.info", resultDetails);

        // Add information to fill the form fields.
        extras.put("data-publication_serial_issn", issn);
        extras.put("authority-publication_serial_issn", authority);
        if (isNotEmpty(eissn)) {
            extras.put("data-publication_serial_eissn", eissn);
            extras.put("authority-publication_serial_eissn", authority);
        }
        extras.put("data-publication_serial_peerReviewed", peerReviewed);
        extras.put("authority-publication_serial_peerReviewed", authority);

        if (isNotEmpty(publisher)) {
            extras.put("data-publication_editor_name", publisher);
            extras.put("authority-publication_editor_name", authority);
        }
        if (isNotEmpty(publisherLocation)) {
            extras.put("data-publication_editor_location", publisherLocation);
            extras.put("authority-publication_editor_location", authority);
        }

        return extras;
    }

    /**
     * Generate a string that will be display for each search result in the frontend.
     * The final format is: "{issn} ({publisher} - {publisherLocation})"
     * 
     * @param issn The issn of the journal.
     * @param publisher The optional publisher of the journal.
     * @param publisherLocation The optional publisher location of the journal.
     * @return The full generated string to display.
     */
    private String getFullDetailString(String issn, String publisher, String publisherLocation) {
        Optional<String> optPublisher = Optional.ofNullable(publisher)
            .filter(s -> (s != null) && !s.trim().isEmpty());
        Optional<String> optLocation = Optional.ofNullable(publisherLocation)
            .filter(s -> (s != null) && !s.trim().isEmpty());

        String publisherInfo = optPublisher
            .map(pub -> String.format("(%s%s)", pub, optLocation.map(loc -> " - " + loc).orElse("")))
            .orElse("");

        return issn + (publisherInfo.isEmpty() ? "" : " " + publisherInfo);
    }

    @Override
    public String getLabel(String key, String locale) {
        try {
            Item journal = itemService.find(getContext(), UUID.fromString(key));
            if (journal != null) {
                String name =  itemService.getMetadataFirstValue(journal, "dc", "title", null, null);
                if (name != null) {
                    return name;
                }
            }
            return key;
        } catch (SQLException e) {
            return key;
        }
    }

    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
