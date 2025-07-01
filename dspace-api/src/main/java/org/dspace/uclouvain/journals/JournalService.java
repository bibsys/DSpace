/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.journals;

import org.dspace.core.Context;

public interface JournalService {
    public Journal findByIssn(Context context, String issn) throws Exception;
}
