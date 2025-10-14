/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.Journal;

public interface JournalService {
    public Journal findByIssn(Context context, String issn) throws Exception;
}
