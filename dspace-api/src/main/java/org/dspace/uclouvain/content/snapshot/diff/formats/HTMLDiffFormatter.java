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
 * Basic implementation for any HTMLDiffFormater subclasses.
 * When a diff must be rendered as HTML:
 *   - each change take part into a specific 'div.diff-section'
 *   - each section has a specific label
 *   - each section has a content part
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class HTMLDiffFormatter<E extends DiffExplainer<?>> implements DiffFormatter<E> {

    @Override
    public String getPrefix(E explainer) {
        return """
            <div class="diff-section">
                <label>%s :: %s</label>
                <div class="diff-content">
        """.formatted(getOperationTypeLabel(explainer), getSectionLabel(explainer));
    }
    protected String getSectionLabel(E explainer) {
        return explainer.getPath();
    }

    protected String getOperationTypeLabel(E explainer) {
        return """
            <span class="operation-type %s">%s</span>
        """.formatted(explainer.getType(), explainer.getType().toUpperCase());
    }

    @Override
    public String getSuffix(E explainer) {
        return "</div></div>";
    }
}
