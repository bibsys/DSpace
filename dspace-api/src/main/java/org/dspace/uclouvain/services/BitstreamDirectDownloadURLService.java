/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.eperson.EPerson;

public interface BitstreamDirectDownloadURLService {
    String getURL(Bitstream bitstream, EPerson ePerson);
    String getURL(Bitstream bitstream, String email);
    List<String> getURLs(Bundle bundle, EPerson ePerson);
    List<String> getURLs(Bundle bundle, String email);
}