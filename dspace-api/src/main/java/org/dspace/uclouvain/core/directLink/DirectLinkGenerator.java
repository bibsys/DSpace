/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.directLink;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * This abstract class is the "parent" class for any direct link generator.
 * The main logic of direct link generator is in this class.
 * To use a direct link generator, there are two main methods:
 *     - `buildUrl`: used to build an url to download a bitstream containing an encrypted token.
 *     - `isTokenValid`: use to validate an encrypted token for a specific bitstream.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class DirectLinkGenerator {

    // CLASS CONSTANTS =================================================================================================
    protected static final Logger logger = LogManager.getLogger(DirectLinkGenerator.class);
    public static final String GLUE = "::";

    // CLASS ATTRIBUTES ================================================================================================
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected String backendURL = configurationService.getProperty("dspace.server.url");

    // ABSTRACT METHODS ================================================================================================

    /**
     * Get the direct link generator type.
     * This value is used by the link generator factory to determine which generator used.
     *
     * @return the direct link type
     */
    public abstract String getLinkType();

    /**
     * Build the URL to use to download a bitstream without any restriction
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to download
     * @param args All required arguments used to build the URL (depending on concrete link generator class)
     * @return The full build URL
     * @throws Exception If any exception occurred during the URL generation
     */
    protected abstract String buildSecret(Context context, Bitstream bitstream, Map<String, Object> args)
        throws Exception;

    /**
     * Validate a token for a specific bitstream.
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to analyze
     * @param token The token to validate
     * @return True if the token is valid, false otherwise (or any exception)
     */
    protected abstract boolean validateToken(Context context, Bitstream bitstream, String token);

    /**
     * Validate if the bitstream can be downloaded.
     * For example, in some cases, bitstream for a workflow/archive item can never be downloaded (despite a valid token)
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to analyze
     * @param token The token to validate
     * @return True if the bitstream can be downloaded, False otherwise (or any exception)
     */
    protected abstract boolean validateBitstream(Context context, Bitstream bitstream, String token);

    // GENERIC METHODS =================================================================================================
    /**
     * Build the URL to use to download a bitstream skipping all restrictions defined by Dspace.
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to analyze
     * @param args A dictionary of arguments required building the URL (each link generator required specific arguments)
     * @return The URL to use to download the bitstream without any policy restrictions.
     * @throws IllegalStateException if required argument is missing
     * @throws Exception for any other kind of exception
     */
    public String buildURL(Context context, Bitstream bitstream, Map<String, Object> args) throws Exception {
        return String.format(
            "%s/api/uclouvain/bitstream/%s/content?hash=%s",
            backendURL,
            bitstream.getID(),
            buildSecret(context, bitstream, args)
        );
    }

    /**
     * Check if a token is valid for the corresponding bitstream.
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to analyze
     * @param token The token to analyze
     * @return if the token is valid for the corresponding bitstream, false otherwise (and for any exception)
     */
    public boolean isTokenValid(Context context, Bitstream bitstream, String token) {
        return this.validateToken(context, bitstream, token) && this.validateBitstream(context, bitstream, token);
    }

    /**
     * Try to find an argument into a dictionary and cast it to a specific class
     *
     * @param args The dictionary to analyze
     * @param key The argument key
     * @param clazz The class used to cast the argument (if found)
     * @return The requested argument
     * @param <T> The casted argument class
     * @throws IllegalStateException if the key doesn't exist into the dictionary
     * @throws ClassCastException if the argument cannot be cast
     */
    protected <T> T getArgument(Map<String, Object> args, String key, Class<T> clazz)
        throws IllegalStateException, ClassCastException {
        if (!args.containsKey(key)) {
            throw new IllegalStateException(key + " argument is missing");
        }
        return clazz.cast(args.get(key));
    }

    /**
     * Build the token that should be encrypted next
     * @param o The object to encode
     * @return the stringify token prefixed by direct link type (ex: limitedTime::epoch_in_millis)
     */
    protected String buildToken(Object o) {
        return this.getLinkType() + GLUE + o;
    }

    /**
     * Extract the representative value from the token.
     * Ex: `limitedTime::epoch_in_millis` will return `epoch_in_millis`
     * @param token The token to analyze
     * @return The token representative value
     * @throws InvalidTokenException if the token isn't valid token for the direct link class
     */
    protected String getTokenValue(String token) throws InvalidTokenException {
        String[] parts = token.split(GLUE, 2);
        String prefix = (parts.length == 2) ? parts[0] : null;
        if (prefix == null || !prefix.equals(getLinkType())) {
            throw new InvalidTokenException("Invalid token for " + this.getLinkType());
        }
        return parts[1];
    }
}