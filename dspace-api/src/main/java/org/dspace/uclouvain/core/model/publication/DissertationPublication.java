/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.dto.RegistrationDataChanges;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Object representing a doctoral dissertation object (text::thesis).
 * With some specific method concerning dissertation metadata.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class DissertationPublication extends Publication {

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String DOCUMENT_TYPE = "text::thesis";

    // CONSTRUCTOR =====================================================================================================
    protected DissertationPublication(Item item) throws InvalidModelEntityTypeException {
        super(item);
    }

    // FUNCTIONS =======================================================================================================
    @Override
    public boolean isWithdrawable() {
        return false;
    }

    /**
     * Get supervisor emails of the publication.
     * @return the list of emails for the publication.
     */
    public List<String> getSupervisorEmails() {
        return getMetadataValues(Publication.ADVISOR_EMAIL_FIELD)
            .stream()
            .filter(email -> !CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(email))
            .filter(email -> email.matches(RegistrationDataChanges.EMAIL_PATTERN))
            .distinct()
            .toList();
    }


    /**
     * Get the encoded dissertation defense date
     * @return the raw value of encoded dissertation defense date
     */
    public String getDefenseDate() {
        return getFirstMetadataValue(Publication.DEFENSE_DATE_FIELD);
    }

    /**
     * Extract year from the dissertation defense
     * @return the defense date year, or -1 if no valid year could be found
     */
    public int getDefenseDateYear() {
        try {
            String dateIssued = this.getDefenseDate();
            String yearPart = dateIssued.substring(0, Math.min(dateIssued.length(), 4));
            return Integer.parseInt(yearPart);
        } catch (Exception e) {  // NullPointerException, ParsingException ...
            return -1;
        }
    }


}
