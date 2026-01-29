/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface UCLouvainCitationsService {

    String ALL_STYLE = "all-style";
    String ALL_FORMAT = "all-format";

    /**
     * Get all citation formats that could be used to generate citation for a specific {@link Item}
     * @param context the current DSpace context.
     * @param item the item to analyze.
     * @return the list for crosswalk IDs that is possible to use to generate citations for the items.
     */
    List<String> getAvailableCitationsCrosswalks(Context context, Item item);

    /**
     * Return a specific citation for the given crosswalk and item.
     * This will only return something if the crosswalk exist and is supported for the given item.
     * @param context The current Dspace context.
     * @param item The item to create a citation for.
     * @param crosswalkID The crosswalk ID of the citation to create.
     * @return The generated citation given a specific item and format. Can return null if format not supported.
     * @throws UnknownCitationFormatException Thrown if the given format does not exist in the system.
     */
    String getCitationForItemByCrosswalk(Context context, Item item, String crosswalkID)
            throws UnknownCitationFormatException;

    /**
     * Return a specific citation for the given format and item.
     * This will only return something if the format exist and is supported for the given item.
     * @param context The current Dspace context.
     * @param item The item to create a citation for.
     * @param style the citation style to use (apa, chicago, fnrs, ... or ALL_STYLE)
     * @param format The citation format to use (html, text, ... or ALL_FORMAT)
     * @return A map of generated citations. Each key is the crosswalk used to generate the citation, each value is the
     *         citation itself.
     * @throws UnknownCitationFormatException Thrown if the given format does not exist in the system.
     */
    Map<String, String> getCitationForItem(Context context, Item item, @NonNull String style, @Nullable String format)
            throws UnknownCitationFormatException;

    default Map<String, String> getCitationForItem(Context context, Item item, @NonNull String style)
            throws UnknownCitationFormatException {
        return getCitationForItem(context, item, style, null);
    }

    /**
     * Evaluate a given format to make sure it is supported by the system.
     * @param context The current DSpace context.
     * @param citationFormat The citation format to evaluate.
     * @return True if the format is supported, false otherwise.
     */
    boolean isValidFormat(Context context, String citationFormat);
}
