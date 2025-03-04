/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest.model.thesis;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.MasterThesis;
import org.dspace.uclouvain.content.MasterThesisAuthor;
import org.dspace.uclouvain.services.MasterThesisServiceImpl;

/** Class to represent basic information about MasterThesis
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ThesisSummary {

    public List<MasterThesisAuthor> authors;
    public List<String> degrees;
    public List<ThesisSummaryDate> dates = new ArrayList<>();
    public String language;
    public String pid;
    public String session;
    public String title;
    public String url;
    public int academicYear;
    public int year;

    private ThesisSummary() {}

    /**
     * Allow parsing an `Item` to create the corresponding `ThesisSummary`.
     * If the item isn't a `MasterThesis`, no summary could be parsed.
     *
     * @param item the Item to parse
     * @return the corresponding ThesisSummary, null if summary cannot be parsed.
     */
    public static ThesisSummary parse(Item item) {
        MasterThesis mtItem = new MasterThesis(item);
        String entityType = mtItem.getMetadataValue("dspace.entity.type");
        if (entityType == null || !entityType.equals(MasterThesisServiceImpl.MASTER_THESIS_ENTITY_TYPE)) {
            return null;
        }

        ThesisSummary summary = new ThesisSummary();
        summary.pid = item.getID().toString();
        summary.language = mtItem.getMetadataValue("dc.language.iso-639-2");
        summary.title = mtItem.getMetadataValue("dc.title");
        summary.authors = mtItem.getAuthors();

        // degree codes
        summary.degrees = new ArrayList<>();
        summary.degrees.addAll(mtItem.getMetadataValues("masterthesis.degree.code"));
        summary.degrees.addAll(mtItem.getMetadataValues("masterthesis.rootdegree.code"));

        // year, academic_year & session
        String yearString = mtItem.getMetadataValue("dc.date.issued");
        if (yearString != null) {
            summary.year = Integer.parseInt(yearString);
            summary.academicYear = summary.year;
        }
        summary.session = mtItem.getMetadataValue("masterthesis.session");
        if (summary.session != null) {
            if (!summary.session.equals("December")) {
                summary.academicYear = summary.year - 1;
            }
            summary.session += " " + summary.year;
        }

        // dates
        String mdValue = mtItem.getMetadataValue("dc.date.created");
        if (mdValue != null) {
            summary.dates.stream().filter(d -> d.type.equals("created")).findFirst().ifPresent(summary.dates::remove);
            summary.dates.add(new ThesisSummaryDate("created", mdValue));
        }
        mdValue = mtItem.getMetadataValue("dc.date.validated");
        if (mdValue != null) {
            summary.dates.stream().filter(d -> d.type.equals("validated")).findFirst().ifPresent(summary.dates::remove);
            summary.dates.add(new ThesisSummaryDate("validated", mdValue));
        }

        // handle
        if (item.getHandle() != null) {
            ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
            String handlePrefix = configService.getProperty("handle.canonical.prefix");
            if (!handlePrefix.endsWith("/")) {
                handlePrefix += '/';
            }
            summary.url = handlePrefix + item.getHandle();
        }

        return summary;
    }
}

class ThesisSummaryDate {

    public String type;
    public String value;

    public ThesisSummaryDate(String type, String value) {
        this.type = type;
        this.value = value;
    }
}
