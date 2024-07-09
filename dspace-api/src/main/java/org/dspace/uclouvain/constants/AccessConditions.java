/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.constants;

/** Enum class to list all possible access conditions on publication's bitstream. */
public class AccessConditions {

    private AccessConditions() {
        throw new UnsupportedOperationException();
    }

    public static final String OPEN_ACCESS = "openaccess";
    public static final String EMBARGO = "embargo";
    public static final String MIXED = "mixed";
    public static final String CLOSED = "administrator";
    public static final String RESTRICTED = "UCLouvain networks restriction";

}
