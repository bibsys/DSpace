/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.Journal;

/**
 * Service uses to work with `Journal` item object.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public interface JournalService {

    /** Find a journal by its title.
     * @param context The current DSpace context.
     * @param journalName the journal title name to search for; `null` if the journal is not found
     * @return the corresponding journal; `null` if the journal is not found
     */
    Journal findByTitle(Context context, String journalName);

    /**
     * Find a journal by its ISSN identifier.
     * @param context The current DSpace context.
     * @param issn The issn of the journal to search for
     * @return the corresponding journal; `null` if the journal is not found
     */
    Journal findByIssn(Context context, String issn);

    /**
     * Find a journal by its e-ISSN identifier.
     * @param context The current DSpace context.
     * @param eissn The eissn of the journal to search for; `null` if the journal is not found
     * @return the corresponding journal; `null` if the journal is not found
     */
    Journal findByEissn(Context context, String eissn);
}
