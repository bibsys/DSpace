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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.directLink.DirectLinkGenerator;
import org.dspace.uclouvain.core.directLink.DirectLinkGeneratorFactory;
import org.dspace.uclouvain.core.directLink.InvalidTokenException;
import org.dspace.uclouvain.utils.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class DirectLinkServiceImpl implements DirectLinkService {

    @Autowired
    DirectLinkGeneratorFactory directLinkGeneratorFactory;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final String password = configService.getProperty("uclouvain.crypto.password");
    private final String salt = configService.getProperty("uclouvain.crypto.password");

    /**
     * Build a valid URL to download bitstream content skipping all DSpace restriction.
     * @param context The Dspace application context
     * @param bitstream The bitstream to allow downloading
     * @param directLinkType The direct link type to use
     * @param args Any arguments required to build the URL depending on direct link type choose.
     * @return The full URL to use to download bitstream content
     * @throws Exception if any exception occurred during URL built.
     */
    @Override
    public String buildURL(Context context, Bitstream bitstream, String directLinkType, Map<String, Object> args)
            throws Exception {
        return directLinkGeneratorFactory.getGenerator(directLinkType).buildURL(bitstream, args);
    }

    /**
     * Try to validate a token/secret for a specific bitstream
     * @param context The Dspace application context
     * @param bitstream The bitstream to download
     * @param hash The encrypted token/secret to validate
     * @return true if the token is valid (then the bitstream content could be downloaded)
     */
    @Override
    public boolean validateToken(Context context, Bitstream bitstream, String hash) throws Exception {
        // We need to decrypt here the token to determine which generator must be used to validate it.
        // The generator type is specified as first part of the decrypted token.
        String decryptedToken = CryptoUtils.decrypt(hash, password, salt);
        String[] parts = decryptedToken.split(DirectLinkGenerator.GLUE, 2);
        if (parts.length != 2) {
            throw new InvalidTokenException("Unable to detect direct link type");
        }
        return directLinkGeneratorFactory.getGenerator(parts[0]).isTokenValid(bitstream, hash);
    }
}
