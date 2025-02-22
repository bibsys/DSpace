/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mail;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.core.Context;

/** Main interface for MailMetadataParserService */
public interface MailMetadataParserService {
    public Pair<String, String> parseMetadata(Context context, Item item, String metadataField, String lang);
    public LinkedHashMap<String, String> parseMetadata(
        Context context, Item item, List<String> metadataFields, String lang
    );
}
