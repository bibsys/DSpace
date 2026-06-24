/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff;

import java.util.List;

/**
 * Class allowing to handle all changes detected between two texts like a "diff" git output
 * A {@link DiffReport} is composed of (at least) one {@link DiffBlock}
 * Each {@link DiffBlock} is composed of multiple {@link DiffSegment}
 *
 * DiffSegment.Type.CONTEXT: Used for words wrapping a change to have better comprehension of this change in large text.
 * DiffSegment.Type.INSERTED: Used for new inserted words into the text.
 * DiffSegment.Type.DELETED: Used for words deleted from the original text.
 * DiffSegment.Type.UPDATED: Used if some words changed from original to revised text (this is the only case for which
 *                           the `revisedText` attribute must be used.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public record DiffReport(List<DiffBlock> blocks) {

    public record DiffBlock(List<DiffSegment> segments) {}

    public record DiffSegment(Type type, String text, String revisedText) {
        public enum Type { CONTEXT, INSERTED, DELETED, UPDATED }
        public DiffSegment(Type type, String text) {
            this(type, text, null);
        }
    }
}
