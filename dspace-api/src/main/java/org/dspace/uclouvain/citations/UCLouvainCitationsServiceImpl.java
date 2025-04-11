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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
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

/**
 * Main service to generate text citations for an item.
 * We use this instead of the classic DSpace system which is using processes and returns files to the user.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainCitationsServiceImpl implements UCLouvainCitationsService {

    private final Logger logger = LogManager.getLogger(UCLouvainCitationsServiceImpl.class);
    private StreamDisseminationCrosswalkMapper crosswalkMapper;
    private static final String CSL_ERROR_REGEX = "\\[CSL STYLE ERROR: .*\\]";

    @Autowired
    ItemExportFormatService itemExportFormatService;
    @Autowired
    ItemService itemService;

    @PostConstruct
    protected void init() {
        this.crosswalkMapper =
            new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
    }

    public boolean isValidFormat(Context context, String citationFormat) {
        return this.crosswalkMapper.getTypes().contains(citationFormat);
    }

    /**
     * Generate a citation for a given format and item.
     * 
     * @param context The current DSpace context.
     * @param item The item to generated a citation for.
     * @param citationFormat The desired format of the citation.
     * 
     */
    public ItemCitation getCitationForItem(Context context, Item item, String citationFormat)
        throws UnknownCitationFormatException {
        // Retrieve the crosswalk for the given format.
        StreamDisseminationCrosswalk citationCrosswalk = this.crosswalkMapper.getByType(citationFormat);
        if (citationCrosswalk == null) {
            throw new UnknownCitationFormatException(citationFormat);
        }

        if (!citationCrosswalk.canDisseminate(context, item)) {
            return null;
        }
        try {
            // Write the result in an output stream and then extract the content from it.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            citationCrosswalk.disseminate(context, item, output);
            String citationResult = output.toString(StandardCharsets.UTF_8);
            logger.debug("Processed citation, got: " + citationResult);
            // The processor may return an error in certain cases.
            if (containsError(citationResult)) {
                // If we detect an error: log it and replace the citation result with null.
                logger.warn(
                    "Could not generate citation of format: [{}] for item: [{}]. Error is: '{}'.",
                    citationFormat, item.getID(), citationResult.trim()
                );
                citationResult = null;
            }
            return new ItemCitation(citationFormat, citationResult);
        } catch (Exception e) {
            logger.error("Citation({}) generation error for {}", citationFormat, item.getID());
            return null;
        }
    }

    /**
     * Retrieve all the citation for all the available formats for the given item.
     * WARNING: It can be very resource consuming if generating a lot of citations.
     *          Use wisely!
     * 
     * @param context The current DSpace context.
     * @param item The item to generate citations for.
     * @return All the possible citations for a given item.
     */
    public List<ItemCitation> getAllCitationsForItem(Context context, Item item) {
        List<ItemCitation> result = new ArrayList<>();
        for (String format: retrieveCitationsFormats(context, itemService.getEntityType(item))) {
            try {
                ItemCitation citation = getCitationForItem(context, item, format);
                if (citation != null) {
                    logger.debug(
                        "Adding citation to final result for format {}, with value '{}'",
                        format,
                        citation.getCitation()
                    );
                    result.add(citation);
                }
            } catch (UnknownCitationFormatException e) {
                logger.warn("Unsupported citation format [" + format + "] in find all format", e);
            }
        }
        return result;
    }

    /**
     * Whether the citation result contains an error.
     *
     * @param citation The citation result to evaluate.
     * @return True if the result contains a string of the form provided by the regex. False otherwise.
     */
    private boolean containsError(String citation) {
        Pattern pattern = Pattern.compile(CSL_ERROR_REGEX);
        return pattern.matcher(citation).find();
    }

    /**
     * Get all valid citations format for a given entityType.
     * 
     * @param context The current DSpace context.
     * @param entityType The entity type to evaluate.
     * @return All the valid formats for a given entity type.
     */
    private List<String> retrieveCitationsFormats(Context context, String entityType) {
        return this.itemExportFormatService
            .byEntityTypeAndMolteplicity(context, entityType, CrosswalkMode.SINGLE)
            .stream()
            .map(ItemExportFormat::getId)
            .collect(Collectors.toList());
    }
}
