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
import org.apache.commons.text.StringEscapeUtils;
import org.dspace.core.I18nUtil;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.explainer.FileDiffExplainer;

/**
 * This class allows to format changes between two snapshot of the same file as HTML.
 * This HTML should be included into "notify_change" email to be well rendered.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@DiffFormatterTypeFor(clazz = FileDiffExplainer.class, format = OutputFormat.EMAIL_HTML)
public class FileHTMLEmailDiffFormatter extends HTMLDiffFormatter<FileDiffExplainer> {

    // CLASS CONSTANTS =================================================================================================
    private static final String LOCALE_OPERATION_PREFIX = "snapshot.email.file.operation.";
    private static final String LOCALE_ACCESS_PREFIX = "snapshot.email.file.access-value.";
    private static final String LINE_UPDATE_BLOCK = """
        <tr>
          <th>UPDATE</th>
          <td class="diff-content">
            <span class="diff-update">%s</span>
          </td>
        </tr>""";

    // INHERITED METHODS ===============================================================================================
    @Override
    public String getPrefix(FileDiffExplainer explainer, Locale locale) {
        String filename = (explainer.getRevised() != null)
            ? explainer.getRevised().getFilename()
            : explainer.getOriginal().getFilename();
        String labelMessage = I18nUtil.getMessage("snapshot.email.file.label", locale).formatted(
            StringEscapeUtils.escapeHtml4(filename),
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
    public String getSuffix(FileDiffExplainer explainer, Locale locale) {
        return """
          </table>
        </li>""";
    }

    @Override
    public String format(FileDiffExplainer explainer, Locale locale) {
        String diffReport = switch (explainer.getType()) {
            case ItemSnapshotDiff.ADD -> formatAddOperation(explainer, locale);
            case ItemSnapshotDiff.REMOVE -> formatRemoveOperation(explainer, locale);
            case ItemSnapshotDiff.UPDATE -> formatUpdateOperation(explainer, locale);
            default -> throw new IllegalStateException("Unexpected type: " + explainer.getType());
        };
        return Stream.of(getPrefix(explainer, locale), diffReport, getSuffix(explainer, locale))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining());
    }

    // PRIVATE METHODS =================================================================================================
    private String formatRemoveOperation(FileDiffExplainer explainer, Locale locale) {
        String localAccess = I18nUtil.getMessage(LOCALE_ACCESS_PREFIX + explainer.getOriginal().getAccess(), locale);
        String localMessage = I18nUtil.getMessage("snapshot.email.file.access-was", locale).formatted(localAccess);
        return """
          <tr>
            <th>OLD</th>
            <td class="diff-content">
              <span class="diff-remove">%s</span>
            </td>
          </tr>
        """.formatted(localMessage);
    }

    private String formatAddOperation(FileDiffExplainer explainer, Locale locale) {
        String localAccess = I18nUtil.getMessage(LOCALE_ACCESS_PREFIX + explainer.getRevised().getAccess(), locale);
        String localMessage = I18nUtil.getMessage("snapshot.email.file.access-is", locale).formatted(localAccess);
        return """
          <tr>
            <th>NEW</th>
            <td class="diff-content">
              <span class="diff-add">%s</span>
            </td>
          </tr>
        """.formatted(localMessage);
    }

    private String formatUpdateOperation(FileDiffExplainer explainer, Locale locale) {
        List<String> parts = new ArrayList<>();
        if (explainer.isFilenameChanges()) {
            parts.add(I18nUtil.getMessage("snapshot.email.file.filename-change", locale).formatted(
                StringEscapeUtils.escapeHtml4(explainer.getOriginal().getFilename()),
                StringEscapeUtils.escapeHtml4(explainer.getRevised().getFilename())
            ));
        }
        if (explainer.isAccessChanges()) {
            String oldAccess = I18nUtil.getMessage(LOCALE_ACCESS_PREFIX + explainer.getOriginal().getAccess(), locale);
            String newAccess = I18nUtil.getMessage(LOCALE_ACCESS_PREFIX + explainer.getRevised().getAccess(), locale);
            parts.add(I18nUtil.getMessage("snapshot.email.file.access-change", locale).formatted(oldAccess, newAccess));
        }
        if (explainer.isContentChanges()) {
            parts.add(I18nUtil.getMessage("snapshot.email.file.content-change", locale));
        }
        return parts.stream().map(LINE_UPDATE_BLOCK::formatted).collect(Collectors.joining());
    }

}
