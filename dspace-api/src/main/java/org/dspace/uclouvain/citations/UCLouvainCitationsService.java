/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface UCLouvainCitationsService {
    /**
     * Generate all the possible citations for a given item.
     * 
     * @param context The current DSpace item.
     * @param item The item to generate citations for.
     * @return A map of all the generated citations for the given item.
     * The key is the format and the value is the citation.
     */
    List<ItemCitation> getAllCitationsForItem(Context context, Item item);
    /**
     * Return a specific citation for the given format and item.
     * This will only return something if the format exist and is supported for the given item.
     * 
     * @param context The current Dspace context.
     * @param item The item to create a citation for.
     * @param citationFormat The format of the citation to create.
     * @return The generated citation given a specific item and format. Can return null if format not supported.
     * @throws UnknownCitationFormatException Thrown if the given format does not exist in the system.
     */
    ItemCitation getCitationForItem(Context context, Item item, String citationFormat)
        throws UnknownCitationFormatException;
    /**
     * Evaluate a given format to make sure it is supported by the system.
     * @param context The current DSpace context.
     * @param citationFormat The citation format to evaluate.
     * @return True if the format is supported, false otherwise.
     */
    boolean isValidFormat(Context context, String citationFormat);
}
