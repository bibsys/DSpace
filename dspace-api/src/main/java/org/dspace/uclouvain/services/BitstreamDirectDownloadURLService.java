package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.eperson.EPerson;

public interface BitstreamDirectDownloadURLService {
    public String getURL(Bitstream bitstream, EPerson ePerson);
    public String getURL(Bitstream bitstream, String email);
    public List<String> getURLs(Bundle bundle, EPerson ePerson);
    public List<String> getURLs(Bundle bundle, String email);
}