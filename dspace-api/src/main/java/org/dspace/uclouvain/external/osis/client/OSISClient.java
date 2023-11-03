package org.dspace.uclouvain.external.osis.client;

import org.dspace.uclouvain.external.osis.model.OSISStudentDegree;

public interface OSISClient {
    OSISStudentDegree[] getOSISStudentDegreeByFGS(String fgs);
}
