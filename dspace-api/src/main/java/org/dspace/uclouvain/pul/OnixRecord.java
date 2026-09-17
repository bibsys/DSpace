/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import java.io.File;
import java.util.List;

import org.jdom2.Element;

/**
 * One PUL ONIX file, read and already turned into DIM by {@link OnixRecordReader}.
 * <p>
 * The identifiers and the title are taken from the DIM, not from the ONIX, so that the stylesheet stays the only
 * definition of the mapping. The notification type is the one ONIX detail the DIM does not carry.
 *
 * @param file             the ONIX file the record comes from
 * @param notificationType ONIX list 1 code: {@code 03} confirmed, {@code 04} update, {@code 05} delete
 * @param gcoi             the PUL primary key ({@code dc.identifier.gcoi}), null if absent
 * @param isbns            every {@code dc.identifier.isbn} of the DIM: the product's own and the other format's
 * @param title            the {@code dc.title}, null if absent
 * @param dim              the {@code <dim:dim>} element ready for ingestion
 * @param unnamedContributors whether the record has a {@code Contributor/UnnamedPersons} ("et al.", "various
 *                         authors", ...): the named contributors are then not the complete list
 */
public record OnixRecord(
    File file,
    String notificationType,
    String gcoi,
    List<String> isbns,
    String title,
    Element dim,
    boolean unnamedContributors
) {

    /** ONIX list 1: the product has been removed from the publisher's catalogue. */
    public static final String NOTIFICATION_DELETE = "05";

    public boolean isDeletion() {
        return NOTIFICATION_DELETE.equals(notificationType);
    }
}
