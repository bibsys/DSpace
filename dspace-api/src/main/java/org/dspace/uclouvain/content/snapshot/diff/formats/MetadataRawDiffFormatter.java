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
 * This class allows to format changes between two snapshot of the same metadata as RAW/TEXT.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@DiffFormatterTypeFor(clazz = MetadataDiffExplainer.class, format = OutputFormat.RAW)
public class MetadataRawDiffFormatter extends RawDiffFormatter<MetadataDiffExplainer> {

    @Override
    public String format(MetadataDiffExplainer explainer) {
        StringJoiner joiner = new StringJoiner(" ... ");
        for (DiffReport.DiffBlock block : explainer.getDiff().blocks()) {
            StringBuilder bs = new StringBuilder();
            for (DiffReport.DiffSegment segment : block.segments()) {
                switch (segment.type()) {
                    case CONTEXT -> bs.append(segment.text());
                    case DELETED -> bs.append(" [[- %s]] ".formatted(segment.text()));
                    case INSERTED -> bs.append(" [[+ %s]] ".formatted(segment.text()));
                    case UPDATED -> bs.append(" [[~ %s -> %s]] ".formatted(segment.text(), segment.revisedText()));
                    default -> throw new IllegalStateException("Unexpected type: " + segment.type());
                }
            }
            joiner.add(bs.toString());
        }
        return Stream.of(getPrefix(explainer), joiner.toString(), getSuffix(explainer))
            .filter(StringUtils::isNoneBlank)
            .collect(Collectors.joining());
    }
}
