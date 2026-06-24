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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.explainer.FileDiffExplainer;

/**
 * This class allows to format changes between two snapshot of the same file as RAW/TEXT.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@DiffFormatterTypeFor(clazz = FileDiffExplainer.class, format = OutputFormat.RAW)
public class FileRawDiffFormatter extends RawDiffFormatter<FileDiffExplainer> {

    @Override
    public String getPrefix(FileDiffExplainer explainer) {
        // No need to include a not human understandable UUID into the rendered message
        return null;
    }

    @Override
    public String format(FileDiffExplainer explainer) {
        String diffReport = switch (explainer.getType()) {
            case ItemSnapshotDiff.ADD -> formatAddOperation(explainer);
            case ItemSnapshotDiff.REMOVE -> formatRemoveOperation(explainer);
            case ItemSnapshotDiff.UPDATE -> formatUpdateOperation(explainer);
            default -> throw new IllegalStateException("Unexpected type: " + explainer.getType());
        };
        return Stream.of(getPrefix(explainer), diffReport, getSuffix(explainer))
            .filter(StringUtils::isNoneBlank)
            .collect(Collectors.joining());
    }

    private String formatAddOperation(FileDiffExplainer explainer) {
        return "New file [%s] is added to the publication. Access is [%s]".formatted(
            explainer.getRevised().getFilename(),
            explainer.getRevised().getAccess()
        );
    }

    private String formatRemoveOperation(FileDiffExplainer explainer) {
        return "The file [%s] has been removed from the publication. Access was [%s]".formatted(
            explainer.getOriginal().getFilename(),
            explainer.getOriginal().getAccess()
        );
    }

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
            ? "The file [%s] has been updated".formatted(explainer.getRevised().getFilename())
            : "The file [%s] has been updated :: %s".formatted(
                explainer.getRevised().getFilename(),
                String.join(", ", parts)
            );
    }

}
