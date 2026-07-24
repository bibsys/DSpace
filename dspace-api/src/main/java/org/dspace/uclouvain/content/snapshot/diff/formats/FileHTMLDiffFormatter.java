/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.formats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.explainer.FileDiffExplainer;

/**
 * This class allows to format changes between two snapshot of the same file as HTML.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@DiffFormatterTypeFor(clazz = FileDiffExplainer.class, format = OutputFormat.HTML)
public class FileHTMLDiffFormatter extends HTMLDiffFormatter<FileDiffExplainer> {

    @Override
    protected String getSectionLabel(FileDiffExplainer explainer) {
        return (explainer.getRevised() != null)
            ? explainer.getRevised().getFilename()
            : explainer.getOriginal().getFilename();
    }

    @Override
    public String format(FileDiffExplainer explainer, Locale locale) {
        String diffReport = switch (explainer.getType()) {
            case ItemSnapshotDiff.ADD -> formatAddOperation(explainer);
            case ItemSnapshotDiff.REMOVE -> formatRemoveOperation(explainer);
            case ItemSnapshotDiff.UPDATE -> formatUpdateOperation(explainer);
            default -> throw new IllegalStateException("Unexpected type: " + explainer.getType());
        };
        return Stream.of(getPrefix(explainer, locale), diffReport, getSuffix(explainer, locale))
            .filter(StringUtils::isNoneBlank)
            .collect(Collectors.joining());
    }

    // TODO :: Localized ....
    private String formatAddOperation(FileDiffExplainer explainer) {
        return "<span class=\"diff-add\">New file [%s] added to the publication. Access is [%s]</span>"
            .formatted(explainer.getRevised().getFilename(), explainer.getRevised().getAccess());
    }

    // TODO :: Localized ....
    private String formatRemoveOperation(FileDiffExplainer explainer) {
        return "<span class=\"diff-remove\">The file [%s] has been removed from the publication. Access was [%s]</span>"
            .formatted(explainer.getOriginal().getFilename(), explainer.getOriginal().getAccess());
    }

    // TODO :: Localized ....
    private String formatUpdateOperation(FileDiffExplainer explainer) {
        List<String> parts = new ArrayList<>();
        if (explainer.isFilenameChanges()) {
            parts.add("filename [%s --> %s]".formatted(
                explainer.getOriginal().getFilename(),
                explainer.getRevised().getFilename())
            );
        }
        if (explainer.isAccessChanges()) {
            parts.add("access [%s --> %s]".formatted(
                explainer.getOriginal().getAccess(),
                explainer.getRevised().getAccess())
            );
        }
        if (explainer.isContentChanges()) {
            parts.add("file content was updated");
        }

        return (parts.isEmpty())
            ? "<span class=\"diff-update\">The file [%s] has been updated</span>".formatted(
                explainer.getRevised().getFilename())
            : "<span class=\"diff-update\">The file [%s] has been updated :: %s</span>".formatted(
                explainer.getRevised().getFilename(),
                String.join(", ", parts));
    }

}
