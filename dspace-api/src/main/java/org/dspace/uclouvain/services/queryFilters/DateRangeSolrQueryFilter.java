/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.queryFilters;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Solr query filter class to manage date issued filter.
 * This filter class allows values describe like
 *    - `2020`               : filter on a single date
 *    - `2020-2022`          : simple range filter between 2 dates
 *    - `2020-*`             : range filter from a starting date (included)
 *    - `*-2020`             : range filter until an ending date (included)
 *    - `2020,2016-2018,...` : filter on multiple dates (each part could also be a range)
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class DateRangeSolrQueryFilter implements SolrQueryFilter, SolrSortOption {

    private final DiscoverySearchFilterFacet dateIssuedFilter;

    private static final Pattern DATE_RANGE_PATTERN =
            Pattern.compile("^(?<startDate>(\\d{4}|\\*))(-(?<endDate>(\\d{4}|\\*)))?$");


    public DateRangeSolrQueryFilter() {
        this.dateIssuedFilter = DSpaceServicesFactory
            .getInstance()
            .getServiceManager()
            .getServiceByName("searchFilterIssued", DiscoverySearchFilterFacet.class);
    }


    /**
     * Parse a query filter corresponding to a date range filter on publication date issued
     * @param value the value to parse
     * @return a Solr query filter that could be used to filter a solr response.
     * @throws ParseException if any exception occurred during value parsing
     */
    @Override
    public String parse(String value) throws ParseException {
        if (value == null) {
            throw new ParseException("Value is null", 0);
        }

        List<String> parts = new ArrayList<>();
        for (String part : value.split(",")) {
            parts.add(parsePart(part));
        }
        return parts.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" OR "));
    }

    /** Return the Solr field to use to sort on. */
    @Override
    public String getSortField() {
        return "dc.date.issued_dt";
    }

    private String parsePart(String part) throws ParseException {
        // It's not relevant to use '*' date range filter. In this case ignore this filter.
        if (part.trim().equals("*")) {
            return null;
        }
        Matcher matcher = DATE_RANGE_PATTERN.matcher(part.trim());
        if (!matcher.matches()) {
            throw new ParseException("Invalid range date format: " + part, 0);
        }
        String startDate = matcher.group("startDate");
        String endDate = matcher.group("endDate");
        String indexField = dateIssuedFilter.getIndexFieldName();

        return (StringUtils.isBlank(endDate))
            ? "%s:%s".formatted(indexField, startDate)
            : "%s:[%s TO %s]".formatted(indexField, startDate, endDate);
    }
}
