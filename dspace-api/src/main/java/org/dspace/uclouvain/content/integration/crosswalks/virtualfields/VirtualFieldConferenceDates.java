/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.virtualfields;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.core.model.publication.SpeechPublication;

/**
 * Implementation of {@link VirtualField} that returns the conference dates related to an {@link Item}.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class VirtualFieldConferenceDates implements VirtualField {

    public String[] getMetadata(Context context, Item item, String fieldName) {
        try {
            Publication publication = PublicationFactory.build(item);
            if (publication instanceof SpeechPublication speech) {
                String dates = Stream.of(speech.getRawConferenceStartDate(), speech.getRawConferenceEndDate())
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("-"));
                return (StringUtils.isNotBlank(dates)) ? new String[] {dates} : new String[0];
            }
        } catch (InvalidModelEntityTypeException e) {
            // do nothing (maybe log ?)
        }
        return new String[0];

    }
}