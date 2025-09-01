/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.directLink;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.apache.http.util.Asserts;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.uclouvain.utils.CryptoUtils;
import org.springframework.stereotype.Component;

/**
 * Direct link generator allowing download for a limited time.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class LimitedTimeDirectLinkGenerator extends DirectLinkGenerator {

    private final static String LINK_TYPE = "limitedTime";

    private final String password = configurationService.getProperty("uclouvain.crypto.password");
    private final String salt = configurationService.getProperty("uclouvain.crypto.salt");

    /**
     * Constructor
     * Check into application configuration to get required parameters.
     * @throws IllegalStateException if required parameter is missing
     */
    LimitedTimeDirectLinkGenerator() throws IllegalStateException {
        Asserts.notNull(this.password, "missing `uclouvain.crypto.password` property");
        Asserts.notNull(this.salt, "missing `uclouvain.crypto.salt` property");
    }

    @Override
    public String getLinkType() {
        return LINK_TYPE;
    }

    /**
     * Build the encrypted secret to use into the URL.
     *     For this class, the secret will concat `bitstreamID` + `limitTimeEpoch`
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to download
     * @param args Arguments must define "days" parameter: the number of days during which the secret will valid.
     * @return the encrypted token/secret to use into the URL.
     * @throws IllegalStateException for missing argument
     * @throws Exception for any other exception
     */
    @Override
    protected String buildSecret(Context context, Bitstream bitstream, Map<String, Object> args) throws Exception {
        long days = this.getArgument(args, "days", Long.class);
        LocalDateTime limitDate = LocalDateTime.now().plusDays(days);
        long limitTimestamp = limitDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String limitedTimeToken = buildToken(bitstream.getID() + GLUE + limitTimestamp);
        return CryptoUtils.encrypt(limitedTimeToken, password, salt);
    }

    /**
     * Validate if a token/secret is still valid comparing current timestamp for the specified bitstream.
     *
     * @param context The DSpace application context
     * @param bitstream The bitstream to analyze
     * @param token The token to validate
     * @return True if the token/secret is valid, False otherwise
     */
    @Override
    protected boolean validateToken(Context context, Bitstream bitstream, String token) {
        long epochInMillis;
        try {
            String decryptedToken = CryptoUtils.decrypt(token, password, salt);
            String tokenValue = getTokenValue(decryptedToken);
            String[] parts = tokenValue.split(GLUE, 2);
            if (parts.length != 2) {
                throw new InvalidTokenException("Unable to parse token");
            }
            if (!(UUID.fromString(parts[0]).equals(bitstream.getID()))) {
                throw new InvalidTokenException("Invalid token(" + token + ") for bitstream#" + bitstream.getID());
            }
            epochInMillis = Long.parseLong(parts[1]);
        } catch (NumberFormatException nfe) {
            logger.warn("Unable to cast epoch value :: " + nfe.getMessage(), nfe);
            return false;
        } catch (Exception e) {
            logger.info("Token validation error :: " + e.getMessage());
            return false;
        }
        LocalDateTime dateFromEpoch = Instant
                .ofEpochMilli(epochInMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return dateFromEpoch.isAfter(LocalDateTime.now());
    }

    @Override
    protected boolean validateBitstream(Context context, Bitstream bitstream, String token) {
        // For this class, we just need to check the bitstream exists and isn't marked as 'deleted'
        try {
            return !bitstream.isDeleted();
        } catch (SQLException sqle) {
            logger.warn("Database error on bitstream [" + bitstream.getID() + "] :: " + sqle.getMessage(), sqle);
            return false;
        }
    }
}

