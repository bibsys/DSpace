/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.Map;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * Service allowing to build and validate a direct access link to bitstream content skipping all DSpace policy
 * restrictions.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface DirectLinkService {

    /**
     * Build a valid URL to download bitstream content skipping all DSpace restriction.
     * @param ctx The Dspace application context
     * @param bitstream The bitstream to allow downloading
     * @param directLinkType The direct link type to use
     * @param args Any arguments required to build the URL depending on direct link type choose.
     * @return The full URL to use to download bitstream content
     * @throws Exception if any exception occurred during URL built.
     */
    String buildURL(Context ctx, Bitstream bitstream, String directLinkType, Map<String, Object> args) throws Exception;

    /**
     * Try to validate a token/secret for a specific bitstream
     * @param context The Dspace application context
     * @param bitstream The bitstream to download
     * @param hash The token/secret to validate
     * @return true if the token is valid (then the bitstream content could be downloaded)
     */
    boolean validateToken(Context context, Bitstream bitstream, String hash) throws Exception;
}
