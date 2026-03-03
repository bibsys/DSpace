/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.queryFilters;

import java.text.ParseException;

import org.dspace.uclouvain.core.model.publication.SpeechPublication;

/**
 * Solr query filter class to manage poster inclusion into query.
 * User could choose to add (or not) conference poster into an export result. In the fact, we only need to manage the
 * "poster not included" statement. If user don't want poster, we explicitly exclude the corresponding document subtype.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class IncludePosterSolrQueryFilter implements SolrQueryFilter {

    /**
     * Parse a query filter corresponding to include poster document subtype or not
     * @param value the value to parse
     * @return a Solr query filter that could be used to filter a solr response.
     * @throws ParseException if any exception occurred during value parsing
     */
    @Override
    public String parse(String value) throws ParseException {
        if (!Boolean.parseBoolean(value)) {
            return "-dc.type.subtype:\"" + SpeechPublication.SUBTYPE_POSTER + "\"";
        }
        return null;
    }
}
