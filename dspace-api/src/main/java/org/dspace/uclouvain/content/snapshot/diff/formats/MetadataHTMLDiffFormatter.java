/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.formats;

import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.content.snapshot.diff.DiffReport;
import org.dspace.uclouvain.content.snapshot.diff.explainer.MetadataDiffExplainer;

/**
 * This class allows to format changes between two snapshot of the same metadata as HTML.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@DiffFormatterTypeFor(clazz = MetadataDiffExplainer.class, format = OutputFormat.HTML)
public class MetadataHTMLDiffFormatter extends HTMLDiffFormatter<MetadataDiffExplainer> {

    @Override
    public String format(MetadataDiffExplainer explainer) {
        StringJoiner joiner = new StringJoiner(" &mldr; "); // this is the html entity for "..."
        for (DiffReport.DiffBlock block : explainer.getDiff().blocks()) {
            StringBuilder bs = new StringBuilder();
            for (DiffReport.DiffSegment segment : block.segments()) {
                switch (segment.type()) {
                    case CONTEXT -> bs.append(segment.text());
                    case DELETED -> bs.append(" <span class=\"diff-remove\">%s</span> ".formatted(segment.text()));
                    case INSERTED -> bs.append("  <span class=\"diff-add\">%s</span> ".formatted(segment.text()));
                    case UPDATED -> bs
                        .append(" <span class=\"diff-remove\">%s</div><div class=\"diff-add\">%s</span> "
                        .formatted(segment.text(), segment.revisedText()));
                    default -> throw new IllegalStateException("Unexpected type: " + segment.type());
                }
            }
            joiner.add(bs.toString());
        }
        return Stream.of(getPrefix(explainer), joiner.toString(), getSuffix(explainer))
            .filter(StringUtils::isNoneBlank)
            .collect(Collectors.joining())
            .trim();
    }
}
