package org.dspace.external.osis.client;

import org.dspace.external.osis.model.OSISStudentDegree;

public interface OSISClient {
    OSISStudentDegree[] getOSISStudentDegreeByFGS(String fgs);
}
