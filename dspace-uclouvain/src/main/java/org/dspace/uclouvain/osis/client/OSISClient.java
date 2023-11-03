package org.dspace.uclouvain.osis.client;

import org.dspace.uclouvain.osis.model.OSISStudentDegree;

public interface OSISClient {
    OSISStudentDegree[] getOSISStudentDegreeByFGS(String fgs);
}
