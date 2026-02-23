/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.csl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.undercouch.citeproc.ListItemDataProvider;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DCPersonName;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.core.model.publication.SpeechPublication;

/**
 * Implementation of {@link ListItemDataProvider} to provide {@link CSLItemData}
 * starting from a DSpace Item with specific behavior according to UCLouvain data
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 *
 */
public class UCLouvainListItemDataProvider extends DSpaceListItemDataProvider {

    public UCLouvainListItemDataProvider(ItemService itemService) {
        super(itemService);
    }

    @Override
    protected CSLItemDataBuilder handleCslDateFields(Item item, CSLItemDataBuilder itemBuilder) {
        super.handleCslDateFields(item, itemBuilder); // Call every classic field filler
        manageEventDate(item, itemBuilder);
        return itemBuilder;
    }

    @Override
    protected CSLItemDataBuilder handleCslNameFields(Item item, CSLItemDataBuilder itemBuilder) {
        try {
            Publication publication = PublicationFactory.build(item);
            List<String> authorsRoles = List.of(
                PublicationAuthor.ROLE_AUTHOR,
                PublicationAuthor.ROLE_FIRST_AUTHOR,
                PublicationAuthor.ROLE_LAST_AUTHOR,
                PublicationAuthor.ROLE_INVENTOR,
                PublicationAuthor.ROLE_PREFACE_WRITER
            );
            consumeAuthorsByRole(publication, authorsRoles, true, itemBuilder::author);
            consumeAuthorsByRole(publication, List.of(PublicationAuthor.ROLE_DIRECTOR), itemBuilder::editorialDirector);
            consumeAuthorsByRole(publication, List.of(PublicationAuthor.ROLE_TRANSLATOR), itemBuilder::translator);
            consumeAuthorsByRole(publication, List.of(PublicationAuthor.ROLE_COLLABORATOR), itemBuilder::contributor);
            // Add default behavior for thesis supervisors and for host document authors
            consumeCSLNamesIfNotBlank(director, item, itemBuilder::director);
            consumeRawNamesIfNotBlank(containerAuthor, item, itemBuilder::containerAuthor);
            consumeRawNamesIfNotBlank(editor, item, itemBuilder::editor);
            return itemBuilder;
        } catch (InvalidModelEntityTypeException e) {
            // If the cast into {@link Publication} isn't possible, then use the default behavior
            return super.handleCslNameFields(item, itemBuilder);
        }
    }


    /**
     * Get conference event date if the item is a conference-speech
     *   Default behavior of {@link DSpaceListItemDataProvider} doesn't permit to check metadata into multiple fields.
     *   Then, if date doesn't contain same part length, we need to harmonize the size to prevent CSL citation
     *   silent error.
     * @param item the {@link Item} to analyze
     * @param itemBuilder the {@link CSLItemDataBuilder} used to create CSL citation
     */
    private void manageEventDate(Item item, CSLItemDataBuilder itemBuilder) {
        // Try to add conference event dates
        //    CSL only provide one field to specify event dates. Using the classic DSpace system, we can't provide
        //    multiple fields. So we use a custom method to populate this data.
        try {
            Publication publication = PublicationFactory.build(item);
            if (publication instanceof SpeechPublication speech) {
                int[][] dates = Stream.of(speech.getRawConferenceStartDate(), speech.getRawConferenceEndDate())
                    .filter(Objects::nonNull)
                    .map(date -> Arrays.stream(date.split("-"))
                        .filter(s -> !s.isEmpty())
                        .mapToInt(Integer::parseInt)
                        .toArray()
                    )
                    .toArray(int[][]::new);
                // To prevent some CSL bugs, we need to harmonize array length. Search for minimal array size, then
                // check and update (if necessary) arrays.
                int minSize = Arrays.stream(dates).mapToInt(row -> row.length).min().orElse(0);
                for (int i = 0; i < dates.length; i++) {
                    dates[i] = Arrays.copyOf(dates[i], minSize);
                }
                // Build CSL fields with computed dates
                itemBuilder.eventDate(new CSLDateBuilder().dateParts(dates).build());
            }
        } catch (Exception e) {
            // pass do nothing...
        }
    }

    private void consumeAuthorsByRole(Publication publication, List<String> roles, Consumer<CSLName[]> consumer) {
        consumeAuthorsByRole(publication, roles, false, consumer);
    }

    private void consumeAuthorsByRole(
            Publication publication,
            List<String> roles,
            boolean includeEtAl,
            Consumer<CSLName[]> consumer
    ) {
        List<CSLName> names = publication
            .getAuthors(roles.toArray(String[]::new))
            .stream()
            .map(this::formatAuthorName)
            .map(super::toCSLName)
            .collect(Collectors.toCollection(ArrayList::new)); // we need a mutable list !

        // If the publication contains not encoded additional authors, add a fake placeholder author with "et al." value
        if (includeEtAl && publication.hasExtraAuthors()) {
            names.add(new CSLNameBuilder().literal("et al.").build());
        }

        if (!names.isEmpty()) {
            consumer.accept(names.toArray(CSLName[]::new));
        }
    }

    private void consumeRawNamesIfNotBlank(String value, Item item, Consumer<CSLName[]> consumer) {
        if (StringUtils.isNotBlank(value)) {
            String mdValue = getMetadataFirstValue(item, value);
            if (StringUtils.isNotBlank(mdValue)) {
                // Create a single CSLName with a 'literal' value containing metadata value
                CSLName data = new CSLNameBuilder().literal(mdValue).build();
                consumer.accept(new CSLName[]{data});
            }
        }
    }

    protected DCPersonName formatAuthorName(PublicationAuthor author) {
        return new DCPersonName(author.getName());
    }
}
