/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.formats;

import java.util.Locale;

import org.dspace.uclouvain.content.snapshot.diff.explainer.DiffExplainer;

/**
 * Contrat that any formatter need to respect to format a snapshot element change.
 *
 * @param <E> the DiffExplainer subclass manage by this formatter (FileDiffExplainer, MetadataDiffExplainer, ...)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface DiffFormatter<E extends DiffExplainer<?>> {
    String getPrefix(E explainer, Locale locale);
    String getSuffix(E explainer, Locale locale);
    String format(E explainer, Locale locale);
}
