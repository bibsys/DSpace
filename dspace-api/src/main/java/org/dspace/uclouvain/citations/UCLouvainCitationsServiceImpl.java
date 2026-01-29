/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.content.integration.crosswalks.service.ItemExportFormat;
import org.dspace.content.integration.crosswalks.service.ItemExportFormatService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Main service to generate text citations for an item.
 * We use this instead of the classic DSpace system which is using processes and returns files to the user.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainCitationsServiceImpl implements UCLouvainCitationsService {

    private static final Logger logger = LogManager.getLogger(UCLouvainCitationsServiceImpl.class);
    private static final String CSL_ERROR_REGEX = "\\[CSL STYLE ERROR: .*\\]";
    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    @Autowired
    ItemExportFormatService itemExportFormatService;
    @Autowired
    ItemService itemService;

    @PostConstruct
    protected void init() {
        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
    }

    public boolean isValidFormat(Context context, String citationFormat) {
        return this.crosswalkMapper.getTypes().contains(citationFormat);
    }

    /**
     * Generate a citation for a given format and item.
     * @param context The current DSpace context.
     * @param item The item to generate a citation for.
     * @param crosswalk The crosswalk key to use to generate the citation.
     * @return the generated citations (null if the citation cannot be generated)
     * @throws UnknownCitationFormatException if the citation format doesn't exist
     */
    public String getCitationForItemByCrosswalk(Context context, Item item, String crosswalk)
        throws UnknownCitationFormatException {
        // Retrieve the crosswalk for the given format.
        StreamDisseminationCrosswalk citationCrosswalk = this.crosswalkMapper.getByType(crosswalk);
        if (citationCrosswalk == null) {
            throw new UnknownCitationFormatException(crosswalk);
        }
        if (!citationCrosswalk.canDisseminate(context, item)) {
            return null;
        }
        try {
            // Write the result in an output stream and then extract the content from it.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            citationCrosswalk.disseminate(context, item, output);
            String citationResult = output.toString(StandardCharsets.UTF_8);
            // The processor may return an error in certain cases.
            if (containsError(citationResult)) {
                // If we detect an error: log it and replace the citation result with null.
                logger.warn(
                    "Could not generate citation of crosswalk: [{}] for item: [{}]. Error is: '{}'.",
                        crosswalk, item.getID(), citationResult.trim()
                );
                citationResult = null;
            }
            return (StringUtils.isNotEmpty(citationResult)) ? citationResult.trim() : citationResult;
        } catch (Exception e) {
            logger.warn("Citation({" + crosswalk + "}) generation error for {" + item.getID() + "}", e);
            return null;
        }
    }

    /**
     * Return a specific citation for the given format and item.
     * This will only return something if the format exist and is supported for the given item.
     * @param context The current Dspace context.
     * @param item The item to create a citation for.
     * @param style the citation style to use (apa, chicago, fnrs, ... or ALL_STYLE)
     * @param format The citation format to use (html, text, ... or ALL_FORMAT)
     * @return A map of generated citations. Each key is the crosswalk used to generate the citation, each value is the
     *         citation itself.
     * @throws UnknownCitationFormatException Thrown if the given format does not exist in the system.
     */
    public Map<String, String> getCitationForItem(Context context, Item item, @NonNull String style, String format)
        throws UnknownCitationFormatException {
        String regex = buildCitationPattern(item, style, format);
        Predicate<String> isMatching = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).asPredicate();
        return getAvailableCitationsCrosswalks(context, item)
            .stream()
            .filter(isMatching)
            .collect(Collectors.toMap(
                crosswalk -> crosswalk,
                crosswalk -> getCitationForItemByCrosswalk(context, item, crosswalk),
                (existing, replacement) -> existing) //(security) if double exists, keep first
            );
    }

    /**
     * Get all valid citations format for a given item.
     * @param context The current DSpace context.
     * @param item The item to evaluate.
     * @return All the valid formats for a given entity type.
     */
    public List<String> getAvailableCitationsCrosswalks(Context context, Item item) {
        String entityType = itemService.getEntityType(item);
        if (StringUtils.isNotBlank(entityType)) {
            return this.itemExportFormatService
                .byEntityTypeAndMolteplicity(context, entityType, CrosswalkMode.SINGLE)
                .stream()
                .map(ItemExportFormat::getId)
                .toList();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Whether the citation result contains an error.
     * @param citation The citation result to evaluate.
     * @return True if the result contains a string of the form provided by the regex. False otherwise.
     */
    private boolean containsError(String citation) {
        Pattern pattern = Pattern.compile(CSL_ERROR_REGEX);
        return pattern.matcher(citation).find();
    }

    /**
     * Build a crosswalk regexp pattern for an item based on style and format
     * @param item the item to analyze
     * @param style the style to use
     * @param format the format to use
     * @return the regexp pattern to use to find matching crosswalk
     */
    private String buildCitationPattern(Item item, @NonNull String style, @Nullable String format) {
        String entityType = itemService.getEntityType(item);
        String stylePart = Objects.equals(style, ALL_STYLE) ? "[^-]+" : Pattern.quote(style);
        StringBuilder sb = new StringBuilder("^").append(entityType).append("-").append(stylePart);
        if (format != null) {  // if no specified format, only plain text format will be returned
            if (Objects.equals(format, ALL_FORMAT)) {
                sb.append("(-[^-]+)?");
            } else if (!Objects.equals(format, "text")) { // we can use 'text' to force a plain text format return
                sb.append("-").append(Pattern.quote(format));
            }
        }
        return sb.append("$").toString();
    }
}
