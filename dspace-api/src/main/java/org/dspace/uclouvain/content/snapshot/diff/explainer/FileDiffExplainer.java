/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.explainer;

import java.util.Objects;

import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;

/**
 * Explainer class for diff change on a publication attached file (bitstream).
 * This utility class should be used to detect/get changes and render the change in the desired format
 * (see {@link org.dspace.uclouvain.content.snapshot.diff.formats.DiffFormatter}) for better usage comprehension
 *
 * @author Renaud Michptte (renaud.michotte@uclouvain.be)
 */
@DiffExplainerFor(FileSnapshotElement.class)
public class FileDiffExplainer extends DiffExplainer<FileSnapshotElement> {

    public FileDiffExplainer(FileSnapshotElement original, FileSnapshotElement revised)
        throws IllegalArgumentException {
        super(original, revised);
    }

    /** Is the access on the file changed between original and revised snapshot */
    public boolean isAccessChanges() {
        return Objects.equals(getType(), ItemSnapshotDiff.UPDATE)
            && !Objects.equals(getOriginal().getAccess(), getRevised().getAccess());
    }

    /** Is file content changed between original and revised snapshot */
    public boolean isContentChanges() {
        return Objects.equals(getType(), ItemSnapshotDiff.UPDATE)
            && !Objects.equals(getOriginal().getChecksum(), getRevised().getChecksum());
    }

    /** Is name of the file changed between original and revised snapshot */
    public boolean isFilenameChanges() {
        return Objects.equals(getType(), ItemSnapshotDiff.UPDATE)
            && !Objects.equals(getOriginal().getFilename(), getRevised().getFilename());
    }
}
