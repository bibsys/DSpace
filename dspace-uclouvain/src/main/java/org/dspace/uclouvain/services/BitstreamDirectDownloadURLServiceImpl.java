package org.dspace.uclouvain.services;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.Hasher;

public class BitstreamDirectDownloadURLServiceImpl implements BitstreamDirectDownloadURLService {
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private String algorithm;
    private String encryptionKey;
    private String backendURL;

    private Hasher hasher;

    public BitstreamDirectDownloadURLServiceImpl() throws NoSuchAlgorithmException {
        this.algorithm =  this.configurationService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5");
        this.encryptionKey = this.configurationService.getProperty("uclouvain.api.bitstream.download.secret", "");
        this.backendURL = this.configurationService.getProperty("dspace.server.url");
        
        this.hasher = new Hasher(this.algorithm, this.encryptionKey);
    }

    public String getURL(Bitstream bitstream, EPerson ePerson) {
        return ePerson != null ? this.getURL(bitstream, ePerson.getEmail()): null;
    }

    public String getURL(Bitstream bitstream, String email) {
        return this.generate(bitstream, this.hasher.processHashAsString(email));
    }

    public List<String> getURLs(Bundle bundle, EPerson ePerson) {
        return ePerson != null ? this.getURLs(bundle, ePerson.getEmail()) : new ArrayList<String>();
    }

    public List<String> getURLs(@NotNull Bundle bundle, @NotNull String email) {
        return bundle.getBitstreams().stream()
            .map(bs -> this.generate(bs, this.hasher.processHashAsString(email)))
            .collect(Collectors.toList());
    }

    private String generate(Bitstream bitstream, String hash) {
        return this.backendURL + "/api/uclouvain/bitstream/" + bitstream.getID() + "/content?hash=" + hash;
    }
}
