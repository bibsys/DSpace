/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.directLink;

import java.util.List;
import java.util.Map;

import org.apache.http.util.Asserts;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.utils.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ThesisSupervisorDirectLinkGenerator extends DirectLinkGenerator {

    public final static String LINK_TYPE = "thesisSupervisor";

    @Autowired
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    @Autowired
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private final String password = configurationService.getProperty("uclouvain.crypto.password");
    private final String salt = configurationService.getProperty("uclouvain.crypto.salt");
    private final String supervisorEmailField = configurationService
            .getProperty("uclouvain.api.bitstream.download.promoterfield", "advisors.email");

    /**
     * Constructor
     * Check into application configuration to get required parameters.
     * @throws IllegalStateException if required parameter is missing
     */
    ThesisSupervisorDirectLinkGenerator() throws IllegalStateException {
        Asserts.notNull(this.password, "missing `uclouvain.crypto.password` property");
        Asserts.notNull(this.salt, "missing `uclouvain.crypto.salt` property");
    }

    @Override
    public String getLinkType() {
        return LINK_TYPE;
    }

    /**
     * Build the URL to use to download a bitstream without any restriction
     *
     * @param bitstream The bitstream to download
     * @param args      All required arguments used to build the URL (depending on concrete link generator class)
     * @return The full build URL
     * @throws Exception If any exception occurred during the URL generation
     */
    @Override
<<<<<<< Updated upstream
    protected String buildSecret(Bitstream bitstream, Map<String, Object> args) throws Exception {
        String email = getArgument(args, "email", String.class);
        String limitedTimeToken = buildToken(bitstream.getID() + GLUE + email);
=======
    protected String buildSecret(Context context, Bitstream bitstream, Map<String, Object> args) throws Exception {
        DSpaceObject dso = bitstreamService.getParentObject(context, bitstream);
        if (!(dso instanceof Item)) {
            throw new Exception("Unable to find item parent ID");
        }
        String limitedTimeToken = buildToken(bitstream.getID() + GLUE + dso.getID().toString());
>>>>>>> Stashed changes
        return CryptoUtils.encrypt(limitedTimeToken, password, salt);
    }

    /**
     * Validate a token for a specific bitstream.
     *     For this class, the decrypted token must represent a supervisor email for the related thesis
     *
     * @param context The Dspace application context
     * @param bitstream The bitstream to analyze
     * @param token     The token to validate
     * @return True if the token is valid, false otherwise (or any exception)
     */
    @Override
    protected boolean validateToken(Context context, Bitstream bitstream, String token) {
        try {
            String decryptedToken = CryptoUtils.decrypt(token, password, salt);
            String tokenValue = getTokenValue(decryptedToken);

            DSpaceObject dso = bitstreamService.getParentObject(context, bitstream);
            if (!(dso instanceof Item item)) {
                throw new Exception("Unable to find bitstream parent item");
            }
            List<String> supervisorsEmails = itemService
                .getMetadataByMetadataString(item, supervisorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .toList();
            return supervisorsEmails.stream().anyMatch(email -> email.equalsIgnoreCase(tokenValue));
        } catch (Exception e) {
            logger.info("Token validation error :: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate if the bitstream can be downloaded.
     *     For this class, the link must only be valid if the item is not yet archived/published.
     *
     * @param context The Dspace application context
     * @param bitstream The bitstream to analyze
     * @param token     The token to validate
     * @return True if the bitstream can be downloaded, False otherwise (or any exception)
     */
    @Override
    protected boolean validateBitstream(Context context, Bitstream bitstream, String token) {
        try {
            DSpaceObject dso = bitstreamService.getParentObject(context, bitstream);
            if (!(dso instanceof Item item)) {
                throw new Exception("Unable to find bitstream parent item");
            }
            return ItemUtils.isWorkflow(context, item);
        } catch (Exception e) {
            logger.info("Bitstream validation error :: " + e.getMessage());
            return false;
        }
    }
}
