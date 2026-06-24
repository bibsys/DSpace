/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.formats;

import org.dspace.uclouvain.content.snapshot.diff.explainer.DiffExplainer;

/**
 * Basic implementation for any RawDiffFormater subclasses.
 * When a diff must be rendered as raw/text, each change:
 *   - should begin with touched path
 *   - should be on a separate line
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class RawDiffFormatter<E extends DiffExplainer<?>> implements DiffFormatter<E> {

    @Override
    public String getPrefix(E explainer) {
        return "%s ::".formatted(explainer.getPath());
    }

    @Override
    public String getSuffix(E explainer) {
        return "\n";
    }
}
