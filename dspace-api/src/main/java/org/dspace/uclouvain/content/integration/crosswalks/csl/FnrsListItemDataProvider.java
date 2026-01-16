/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.csl;

import java.util.Objects;

import de.undercouch.citeproc.ListItemDataProvider;
import de.undercouch.citeproc.csl.CSLDate;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.core.model.publication.Publication;

/**
 * Implementation of {@link ListItemDataProvider} to provide {@link CSLItemData}
 * starting from a DSpace Item with some specific changes to match FNRS requirements
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class FnrsListItemDataProvider extends UCLouvainListItemDataProvider {

    private final static String IN_PRESS_LABEL = "in press";

    public FnrsListItemDataProvider(ItemService itemService) {
        super(itemService);
    }

    @Override
    public CSLItemDataBuilder handleAdditionalFields(Item item, CSLItemDataBuilder itemBuilder) {
        manageIssueDate(item, itemBuilder);
        return itemBuilder;
    }

    /**
     * FNRS rules required that if the document status is "accepted/in-press" then the issued date should be replaced
     * by "in press" static string
     * @param item the {@link Item} to analyze
     * @param itemBuilder the {@link CSLItemDataBuilder} used to build the citation
     */
    private void manageIssueDate(Item item, CSLItemDataBuilder itemBuilder) {
        CSLItemData tempData = itemBuilder.build();
        if (Objects.equals(tempData.getStatus(), Publication.STATUS_INPRESS)) {
            // DEV NOTES:
            //   It's not possible to update the previously stored CSLDate, so we need to rebuild a new one with
            //   previous values but force IN_PRESS_LABEL as literal AND remove any date parts.
            //   The logic of (default) CSL engine is that if a 'literal' exist on a data, this literal is the primary
            //   data to exposed... but citeproc:3.0 doesn't implement this fact and always use date parts unless this
            //   section is null (!!! empty is not null for CSL engine)
            CSLDate tempIssued = tempData.getIssued();
            itemBuilder.issued(new CSLDate(
                null,
                tempIssued.getSeason(),
                tempIssued.getCirca(),
                IN_PRESS_LABEL,
                tempIssued.getRaw()
            ));
        }
    }
}
