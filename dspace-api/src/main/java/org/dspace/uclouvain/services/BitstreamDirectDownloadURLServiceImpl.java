/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.Hasher;

public class BitstreamDirectDownloadURLServiceImpl implements BitstreamDirectDownloadURLService {

    private final String backendURL;
    private final Hasher hasher;

    public BitstreamDirectDownloadURLServiceImpl() throws NoSuchAlgorithmException {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String algorithm = configService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5");
        String encryptionKey = configService.getProperty("uclouvain.api.bitstream.download.secret", "");
        backendURL = configService.getProperty("dspace.server.url");
        hasher = new Hasher(algorithm, encryptionKey);
    }

    public String getURL(Bitstream bitstream, EPerson ePerson) {
        return (ePerson != null)
            ? getURL(bitstream, ePerson.getEmail())
            : null;
    }

    public String getURL(Bitstream bitstream, String email) {
        return generate(bitstream, hasher.processHashAsString(email));
    }

    public List<String> getURLs(Bundle bundle, EPerson ePerson) {
        return (ePerson != null)
            ? getURLs(bundle, ePerson.getEmail())
            : new ArrayList<>();
    }

    public List<String> getURLs(@NotNull Bundle bundle, @NotNull String email) {
        return bundle.getBitstreams().stream()
            .map(b -> generate(b, hasher.processHashAsString(email)))
            .collect(Collectors.toList());
    }

    private String generate(Bitstream bitstream, String hash) {
        return backendURL + "/api/uclouvain/bitstream/" + bitstream.getID() + "/content?hash=" + hash;
    }
}
