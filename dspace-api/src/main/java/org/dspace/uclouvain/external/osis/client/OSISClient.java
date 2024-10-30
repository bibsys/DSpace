/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.osis.client;

import org.dspace.uclouvain.external.osis.model.OSISStudentDegree;

public interface OSISClient {
    OSISStudentDegree[] getOSISStudentDegreeByFGS(String fgs);
}
