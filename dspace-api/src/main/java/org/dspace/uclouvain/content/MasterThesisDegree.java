/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

public class MasterThesisDegree {
    public String degreeCode;
    public String degreeLabel;
    public String rootDegreeCode;
    public String rootDegreeLabel;
    public int place;

    public String toString() {
        return "Code: %s, Label: %s, RootCode: %s, RootLabel: %s".formatted(
            degreeCode, degreeLabel, rootDegreeCode, rootDegreeLabel);
    }
}
