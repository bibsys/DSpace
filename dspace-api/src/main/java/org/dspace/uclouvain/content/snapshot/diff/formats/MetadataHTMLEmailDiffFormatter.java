/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.formats;

import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.dspace.core.I18nUtil;
import org.dspace.uclouvain.content.snapshot.diff.DiffReport;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.explainer.MetadataDiffExplainer;

/**
 * This class allows to format changes between two snapshot of the same metadata as HTML.
 * This HTML should be included into "notify_change" email to be well rendered.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@DiffFormatterTypeFor(clazz = MetadataDiffExplainer.class, format = OutputFormat.EMAIL_HTML)
public class MetadataHTMLEmailDiffFormatter extends HTMLDiffFormatter<MetadataDiffExplainer> {

    // CLASS CONSTANTS =================================================================================================
    private static final Pattern METADATA_INDEX_PATTERN = Pattern.compile("^(.+)\\[(\\d+)]$");
    private static final String LOCALE_METADATA_PREFIX = "snapshot.email.metadata.";
    private static final String LOCALE_OPERATION_PREFIX = "snapshot.email.metadata.operation.";

    // INHERITED METHODS ===============================================================================================
    @Override
    public String getPrefix(MetadataDiffExplainer explainer, Locale locale) {
        Matcher matcher = METADATA_INDEX_PATTERN.matcher(explainer.getPath());
        boolean matches = matcher.find();
        String metadataPath = (matches) ? matcher.group(1) : explainer.getPath();
        int metadataPosition = (matches) ? Integer.parseInt(matcher.group(2)) : 0;
        metadataPath = metadataPath.replaceAll("\\.", "-");

        String labelMessage = I18nUtil.getMessage("snapshot.email.metadata.label", locale).formatted(
            I18nUtil.getMessage(LOCALE_METADATA_PREFIX + metadataPath, locale),
            metadataPosition + 1,
            I18nUtil.getMessage(LOCALE_OPERATION_PREFIX + explainer.getType(), locale)
        );

        return """
          <li style="margin-bottom: 12px">
            <div>%s</div>
            <table role="presentation" cellspacing="0" cellpadding="4" border="0" class="changes"
                   style="margin-top: 6px; width: 100%%; border-collapse: collapse;
                          font-family: 'Courier New', Courier, monospace; font-size: 12px;">
          """.formatted(labelMessage);
    }

    @Override
    public String getSuffix(MetadataDiffExplainer explainer, Locale locale) {
        return """
          </table>
        </li>""";
    }

    @Override
    public String format(MetadataDiffExplainer explainer, Locale locale) {
        String diffReport = switch (explainer.getType()) {
            case ItemSnapshotDiff.REMOVE -> formatRemove(explainer, locale);
            case ItemSnapshotDiff.ADD -> formatAdd(explainer, locale);
            case ItemSnapshotDiff.UPDATE -> formatUpdate(explainer, locale);
            default -> throw new IllegalStateException("Unexpected value: " + explainer.getType());
        };
        return Stream.of(getPrefix(explainer, locale), diffReport, getSuffix(explainer, locale))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining());
    }

    // PRIVATE METHODS =================================================================================================
    private String formatRemove(MetadataDiffExplainer explainer, Locale locale) {
        return """
          <tr>
            <th>OLD</th>
            <td class="diff-content">
              <span class="diff-remove line-through">%s</span>
            </td>
          </tr>
        """.formatted(StringEscapeUtils.escapeHtml4(explainer.getOriginal().getValue()));
    }

    private String formatAdd(MetadataDiffExplainer explainer, Locale locale) {
        return """
          <tr>
            <th>NEW</th>
            <td class="diff-content">
              <span class="diff-add">%s</span>
            </td>
          </tr>
        """.formatted(StringEscapeUtils.escapeHtml4(explainer.getRevised().getValue()));
    }

    private String formatUpdate(MetadataDiffExplainer explainer, Locale locale) {
        return """
          <tr>
            <th>OLD</th>
            <td class="diff-content">%s</td>
          </tr>
          <tr>
            <th>NEW</th>
            <td class="diff-content">%s</td>
          </tr>
        """.formatted(getOldString(explainer), getNewString(explainer));
    }

    private String getOldString(MetadataDiffExplainer explainer) {
        // DEV NOTES :: For old string, we need to display any blocks as they were before changes. So we skip "INSERTED"
        // segments, and all other ones are displayed without any highlights
        StringJoiner joiner = new StringJoiner(" … ");
        for (DiffReport.DiffBlock block : explainer.getDiff().blocks()) {
            StringBuilder bs = new StringBuilder();
            for (DiffReport.DiffSegment segment : block.segments()) {
                switch (segment.type()) {
                    case CONTEXT, DELETED, UPDATED -> bs.append(" %s ".formatted(segment.text()));
                    default -> { }
                }
            }
            String blockText = bs.toString().trim();
            if (StringUtils.isNotBlank(blockText)) {
                joiner.add(blockText);
            }
        }
        return StringEscapeUtils.escapeHtml4(joiner.toString().trim());
    }

    private String getNewString(MetadataDiffExplainer explainer) {
        StringJoiner joiner = new StringJoiner(" … ");
        for (DiffReport.DiffBlock block : explainer.getDiff().blocks()) {
            StringBuilder bs = new StringBuilder();
            for (DiffReport.DiffSegment segment : block.segments()) {
                switch (segment.type()) {
                    case CONTEXT -> bs.append(StringEscapeUtils.escapeHtml4(segment.text()));
                    case DELETED -> bs
                            .append(" <span class=\"diff-remove line-through\">%s</span> "
                            .formatted(StringEscapeUtils.escapeHtml4(segment.text())));
                    case INSERTED -> bs
                            .append("  <span class=\"diff-add\">%s</span> "
                            .formatted(StringEscapeUtils.escapeHtml4(segment.text())));
                    case UPDATED -> bs
                            .append(
                                " <span class=\"diff-remove line-through\">%s</span><span class=\"diff-add\">%s</span> "
                                .formatted(
                                    StringEscapeUtils.escapeHtml4(segment.text()),
                                    StringEscapeUtils.escapeHtml4(segment.revisedText())
                                )
                            );
                    default -> { }
                }
            }
            String blockText = bs.toString().trim();
            if (StringUtils.isNotBlank(blockText)) {
                joiner.add(blockText);
            }
        }
        return joiner.toString().trim();
    }
}
