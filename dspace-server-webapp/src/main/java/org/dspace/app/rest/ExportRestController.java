/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.utils.HttpHeadersInitializer;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.uclouvain.export.result.ExportResult;
import org.dspace.uclouvain.export.services.UCLouvainExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/uclouvain/export")
public class ExportRestController {

    private final Logger logger = LogManager.getLogger(ExportRestController.class);

    @Autowired
    UCLouvainExportService uclouvainExportService;

    /**
     * This endpoint allows to generate a PDF document including valid FWB publication for an author.
     * You can use some "filters" parameters (check dedicated function to know how to use)
     * Example:
     *    /fwb?fgs=12345[&year=2020-2024]
     *    /fwb?uuid=0000-1122-3344-5555&startDate2020&endDate=2024&documentType=text::book,text::book-part
     * @param context The DSpace application context
     * @param response The HTTP response object to used for output
     * @param request The HTTP request object (that could be used to analyze complete request arguments, param, ...)
     * @param uuid the uuid of a ResearchProfile item corresponding to an author
     * @param fgs the FGS identifier of an author
     * @return the valid publications, regarding FWB rules, as a PDF document
     * @throws Exception if any exception occurred
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @GetMapping(value = "/fwb")
    public ResponseEntity<StreamingResponseBody> fwbExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "uuid", required = false) String uuid,
        @RequestParam(value = "fgs", required = false) String fgs
    ) throws Exception {
        if (fgs == null && uuid == null) {
            throw new DSpaceBadRequestException("No author identifiers were provided.");
        }
        Map<String, String> filters = extractFiltersQueryFromRequest(request);
        ExportResult result = uclouvainExportService.getAuthorFWBBibliography(context, uuid, fgs, filters);
        return parseResponse(result, request, response);
    }

    /**
     * This endpoint allows to generate a PDF document including valid FNRS publication for an author.
     * You can use some "filters" parameters (check dedicated function to know how to use)
     * Example:
     *    /fnrs?fgs=12345[&year=2020-2024]
     *    /fnrs?uuid=0000-1122-3344-5555&startDate2020&endDate=2024&documentType=text::book,text::book-part
     * @param context The DSpace application context
     * @param response The HTTP response object to used for output
     * @param request The HTTP request object (that could be used to analyze complete request arguments, param, ...)
     * @param uuid the uuid of a ResearchProfile item corresponding to an author
     * @param fgs the FGS identifier of an author
     * @return the valid publications, regarding FNRS rules, as a PDF document
     * @throws Exception if any exception occurred
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @GetMapping(value = "/fnrs")
    public ResponseEntity<StreamingResponseBody> fnrsExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "uuid", required = false) String uuid,
        @RequestParam(value = "fgs", required = false) String fgs
    ) throws Exception {
        if (fgs == null && uuid == null) {
            throw new DSpaceBadRequestException("No author identifiers were provided.");
        }
        Map<String, String> filters = extractFiltersQueryFromRequest(request);
        ExportResult result = uclouvainExportService.getAuthorFNRSBibliography(context, uuid, fgs, filters);
        return parseResponse(result, request, response);
    }

    /**
     * This endpoint allows to export publications using a specific crosswalk based on a custom query.
     * This query can combine multiple search criteria.
     * You can use some "filters" and "sort" parameters (check dedicated function to know how to use)
     * Example:
     *    /custom?crosswalk=publication-apa&query=field1::value1,field2::value2&operator=or
     *    /custom?crosswalk=publication-apa&query=field1::value1[&filters...&sort=]
     * @param context The DSpace application context
     * @param response The HTTP response object to used for output
     * @param request The HTTP request object (that could be used to analyze complete request arguments, param, ...)
     * @param crosswalkName The 'crosswalk' queryString parameter value [required]
     * @param query the 'query' queryString parameter value [required]
     * @param operator the 'operator' queryString parameter value [optional, default=AND]
     * @param sort the term to use to sort publications
     * @param direction the sorting direction (ASC or DESC)
     * @return the requested publications using crosswalk transformation
     *         (depending on crosswalk, could return any mime-type streamed content)
     * @throws Exception if any exception occurred
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @GetMapping(value = "/custom")
    public ResponseEntity<StreamingResponseBody> customExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "crosswalk") String crosswalkName,
        @RequestParam(value = "query") String query,
        @RequestParam(value = "operator", required = false, defaultValue = "and")
            UCLouvainExportService.QueryOperator operator,
        @RequestParam(value = "sort", required = false, defaultValue = "year") UCLouvainExportService.SortOption sort,
        @RequestParam(value = "direction", required = false, defaultValue = "asc") DiscoverQuery.SORT_ORDER direction
    ) throws Exception {
        Map<String, String> filters = extractFiltersQueryFromRequest(request);
        ExportResult result = uclouvainExportService.getExportResult(
            context,
            crosswalkName,
            parseQuery(query),
            operator,
            filters,
            sort,
            direction
        );
        return parseResponse(result, request, response);
    }

    /**
     * This endpoint allows to export publications using a specific crosswalk for specific author(s)
     * You can use some "filters" and "sort" parameters (check dedicated function to know how to use)
     * Example:
     *    /byAuthor?crosswalk=publication-apa&uuid=0000-1122-3344-5555&fgs=12345
     *    /byAuthor?crosswalk=publication-apa&name=Smith,%20John&name=Doe,%20John[&filters...&sort=]
     * @param context The DSpace application context
     * @param response The HTTP response object to used for output
     * @param request The HTTP request object (that could be used to analyze complete request arguments, param, ...)
     * @param crosswalkName The 'crosswalk' queryString parameter value [required]
     * @param sort the term to use to sort publications
     * @param direction the sorting direction (ASC or DESC)
     * @return the requested publications using crosswalk transformation
     *         (depending on crosswalk, could return any mime-type streamed content)
     * @throws Exception if any exception occurred
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @GetMapping(value = "/byAuthor")
    public ResponseEntity<StreamingResponseBody> byAuthorExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "crosswalk") String crosswalkName,
        @RequestParam(value = "sort", required = false, defaultValue = "year") UCLouvainExportService.SortOption sort,
        @RequestParam(value = "direction", required = false, defaultValue = "asc") DiscoverQuery.SORT_ORDER direction
    ) throws Exception {
        // We can't use `@RequestParam` because Spring split parameter "Name, Firstname" as 2 separated params.
        // To solve this problem with easy solution, use classic `request.getParameterValues` method.
        Map<String, List<String>> rawParams = Map.of(
            "uuid", List.of(Optional.ofNullable(request.getParameterValues("uuid")).orElse(new String[0])),
            "fgs",  List.of(Optional.ofNullable(request.getParameterValues("fgs")).orElse(new String[0])),
            "orcid", List.of(Optional.ofNullable(request.getParameterValues("orcid")).orElse(new String[0])),
            "name", List.of(Optional.ofNullable(request.getParameterValues("name")).orElse(new String[0]))
        );
        List<Pair<String, String>> identifiers = rawParams
            .entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream().map(val -> Pair.of(entry.getKey(), val)))
            .toList();
        if (identifiers.isEmpty()) {
            throw new DSpaceBadRequestException("No author identifiers were provided");
        }
        Map<String, String> filters = extractFiltersQueryFromRequest(request);
        ExportResult result = uclouvainExportService.findByAuthor(
            context,
            identifiers,
            crosswalkName,
            filters,
            sort,
            direction
        );
        return parseResponse(result, request, response);
    }

    /**
     * This endpoint allows to export publications using a specific crosswalk for specific entity(s)
     * You can use some "filters" and "sort" parameters (check dedicated function to know how to use)
     *
     * Using export based on affiliation "name":
     *   we can just return publications linked to this entity name (because multiple institution can share the same
     *   entity name and/or external institution name are not registered into UCLouvain Dspace database)
     * Using export based on affiliation "uuid":
     *   2 behaviors are possible. Either we export publications exactly linked to this entity. Either we export
     *   publication linked to this entity AND all descendant entities. To determine which behavior to choose, we
     *   need to match the 'exact' parameter from queryString. If this parameter is missing, then descendant will
     *   be included.
     *
     * Example:
     *    /byAffiliation?crosswalk=publication-apa&uuid=0000-1122-3344-5555&fgs=12345
     *    /byAffiliation?crosswalk=publication-apa&name=Smith,%20John&name=Doe,%20John[&filters...&sort=]
     * @param context The DSpace application context
     * @param response The HTTP response object to used for output
     * @param request The HTTP request object (that could be used to analyze complete request arguments, param, ...)
     * @param crosswalkName The 'crosswalk' queryString parameter value [required]
     * @param names The list of affiliations names to search for (mutual exclusion with `uuids`)
     * @param uuids the list of affiliations UUID to search for (mutual exclusion with `names`)
     * @param exact is the search must include or not the descendant entities (only with `uuids`, default is 'false')
     * @param sort the term to use to sort publications
     * @param direction the sorting direction (ASC or DESC)
     * @return the requested publications using crosswalk transformation
     *         (depending on crosswalk, could return any mime-type streamed content)
     * @throws Exception if any exception occurred
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @GetMapping(value = "/byAffiliation")
    public ResponseEntity<StreamingResponseBody> byDepartment(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "crosswalk") String crosswalkName,
        @RequestParam(value = "uuid", required = false) List<String> uuids,
        @RequestParam(value = "name", required = false) List<String> names,
        @RequestParam(value = "exact", required = false, defaultValue = "false") boolean exact,
        @RequestParam(value = "sort", required = false, defaultValue = "year") UCLouvainExportService.SortOption sort,
        @RequestParam(value = "direction", required = false, defaultValue = "asc") DiscoverQuery.SORT_ORDER direction
    ) throws Exception {
        // First we check `uuid` and `name` parameters
        //   At least one of them must be present into queryString
        //   Both cannot be present into queryString
        if (uuids == null && names == null) {
            throw new DSpaceBadRequestException("No entity was provided");
        }
        uuids = (uuids != null) ? uuids : Collections.emptyList();
        names = (names != null) ? names : Collections.emptyList();
        if (uuids.isEmpty() && names.isEmpty()) {
            throw new DSpaceBadRequestException("No entity was provided");
        }
        if (!uuids.isEmpty() && !names.isEmpty()) {
            throw new DSpaceBadRequestException("Too many argument was provided. Use only 'uuid' or 'name'");
        }
        // Get possible filters and call the correct service to get export result
        Map<String, String> filters = extractFiltersQueryFromRequest(request);
        ExportResult result = (!names.isEmpty())
            ? uclouvainExportService.findByAffiliationByName(context, names, crosswalkName, filters, sort, direction)
            : uclouvainExportService.findByAffiliationByUUID(
                    context, uuids, !exact, crosswalkName, filters, sort, direction);
        return parseResponse(result, request, response);
    }

    /**
     * This endpoint allows to export publications using a specific crosswalk for specific funding data
     * You can use some "filters" and "sort" parameters (check dedicated function to know how to use)
     * Example:
     *    /byFunding?crosswalk=publication-apa&organization=EU&program=HorizonEurope
     *    /byFunding?crosswalk=publication-apa&organisation=FNRS[&filters...&sort=]
     * @param context The DSpace application context
     * @param response The HTTP response object to used for output
     * @param request The HTTP request object (that could be used to analyze complete request arguments, param, ...)
     * @param crosswalkName The 'crosswalk' queryString parameter value [required]
     * @param organization the funding organization to search for
     * @param program the funding program to search for (a subset of funding organization)
     * @param sort the term to use to sort publications
     * @param direction the sorting direction (ASC or DESC)
     * @return the requested publications using crosswalk transformation
     *         (depending on crosswalk, could return any mime-type streamed content)
     * @throws Exception if any exception occurred
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @GetMapping(value = "/byFunding")
    public ResponseEntity<StreamingResponseBody> byFunding(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "organization") String organization,
        @RequestParam(value = "program", required = false) String program,
        @RequestParam(value = "crosswalk") String crosswalkName,
        @RequestParam(value = "sort", required = false, defaultValue = "year") UCLouvainExportService.SortOption sort,
        @RequestParam(value = "direction", required = false, defaultValue = "asc") DiscoverQuery.SORT_ORDER direction
    ) throws Exception {
        Map<String, String> filters = extractFiltersQueryFromRequest(request);
        ExportResult result = uclouvainExportService.findByFunding(
            context,
            organization,
            program,
            crosswalkName,
            filters,
            sort,
            direction
        );
        return parseResponse(result, request, response);
    }

    // PRIVATE METHODS -------------------------------------------------------------------------------------------------

    /**
     * Parse a complex query filter parameter to a list of query filters term.
     * Example:
     *   INPUT= "dc.title::foo\,bar,dc.keyword::keyword1"
     *   OUTPUT= [("dc.title", "foo,bar"), ("dc.keyword", "keyword1")]
     * @param queryParam the query param to analyze
     * @return a list of decoded query filters
     * @throws IllegalArgumentException if any exception occurred during query analyze
     */
    private List<Pair<String, String>> parseQuery(String queryParam) {
        if (queryParam == null || queryParam.isEmpty()) {
            return List.of();
        }
        // Split using "," character only if not prefixed with "\"; we need to double "\" to protect it in java
        String[] filterParts = queryParam.split("(?<!\\\\),");
        return Arrays.stream(filterParts)
            .map(part -> {
                String[] pair = part.split("::", 2);
                if (pair.length != 2) {
                    throw new IllegalArgumentException("Invalid filter query: " + part);
                }
                String key = pair[0].trim();
                String value = pair[1].replace("\\,", ","); // clean value replacing "\," by simple ","
                return Pair.of(key, value);
            })
            .toList();
    }

    /**
     * Parse the export result into a ResponseBody that can be streamed to the final user.
     * @param result The export result containing export data.
     * @param request The request object to generate the headers.
     * @param response The response object to generate the headers.
     * @return A full response entity that can be sent to the final user?
     * @throws IOException If any error generating the headers.
     */
    private ResponseEntity<StreamingResponseBody> parseResponse(
        ExportResult result, HttpServletRequest request, HttpServletResponse response
    ) throws IOException {
        HttpHeaders headers = generateHeaders(result, request, response);
        return ResponseEntity.ok().headers(headers).body(getResponseBody(result));
    }

    /**
     * Generate a lambda function for a StreamingResponseBody object.
     * This function streams the data of the export to the output stream of the response.
     * @param result The export result containing data to export.
     */
    private StreamingResponseBody getResponseBody(ExportResult result) {
        return outputStream -> {
            try (InputStream in = result.readStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
            } catch (IOException e) {
                logger.warn("Streaming aborted by client: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Error during streaming export", e);
            } finally {
                try {
                    result.close();
                } catch (Exception e) {
                    logger.error("Failed to close ExportResult", e);
                }
            }
        };
    }

    /**
     * Generate headers for a given export result. Set correct content-length, content-type and content-disposition.
     * @param result The export result.
     * @param request The request being made.
     * @param response The response to send to the client.
     * @return An object containing all parsed headers.
     * @throws IOException if the headers are not valid.
     */
    private HttpHeaders generateHeaders(
        ExportResult result, HttpServletRequest request, HttpServletResponse response
    ) throws IOException {
        HttpHeadersInitializer headersInitializer = new HttpHeadersInitializer()
                .withLength(result.getSize())
                .withFileName(result.getFileName())
                .withMimetype(result.getMimeType())
                .withDisposition(buildContentDispositionString(result.getFileName()))
                .with(request)
                .with(response);
        return headersInitializer.initialiseHeaders();
    }

    /**
     * Build a full content-disposition string for a given file name.
     */
    private String buildContentDispositionString(String fileName) {
        return ContentDisposition.builder(HttpHeadersInitializer.CONTENT_DISPOSITION_INLINE)
                .filename(fileName)
                .build()
                .toString();
    }

    /**
     * Useful methods used to extract some filters from an HTTP request and parse them into a map of filters
     * to use to limit a search result
     * @param request the request to analyze
     * @return the map of filters to apply on a basic search to limit result.
     */
    private Map<String, String> extractFiltersQueryFromRequest(HttpServletRequest request) {
        Map<String, String> rawFilters = new HashMap<>();
        rawFilters.put("documentType", extractDocumentTypeFilterQueryFromRequest(request));
        rawFilters.put("year", extractYearRangeFilterQueryFromRequest(request));
        rawFilters.put("includePoster", extractPosterFilterQueryFromRequest(request));
        return rawFilters.entrySet().stream()
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String extractDocumentTypeFilterQueryFromRequest(HttpServletRequest request) {
        String[] paramValues = request.getParameterValues("documentType");
        if (paramValues == null) {
            return null;
        }
        return Arrays.stream(paramValues)                   // ["foo ,bar", "bar", " zoo"]
            .flatMap(val -> Arrays.stream(val.split(",")))  // ["foo ", "bar", "bar", " zoo"]
            .map(String::trim)                              // ["foo", "bar", "bar", "zoo"]
            .filter(StringUtils::isNotBlank)                // remove empty values
            .distinct()                                     // ["foo", "bar", "zoo"]
            .collect(Collectors.joining(","));              // "foo,bar,zoo"
    }

    private String extractYearRangeFilterQueryFromRequest(HttpServletRequest request) {
        // 1. Max priority: yearRange.
        //   If user provide this query argument, just use it and don't check any other query argument
        String yearRange = request.getParameter("yearRange");
        if (StringUtils.isNotBlank(yearRange)) {
            return yearRange.trim();
        }
        // 2. Second priority: year
        //   Collect all `year` parameters and join distinct values by ',' glue character
        String[] years = request.getParameterValues("year");
        if (years != null && years.length > 0) {
            return Arrays.stream(years)
                .flatMap(val -> Arrays.stream(val.split(",")))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
        }

        // 3. Third priority: startYear and/or endYear
        //   Collect a single argument for both `startYear` and `endYear`.
        //   If any argument is present, create the best possible range using them.
        String start = request.getParameter("startYear");
        String end = request.getParameter("endYear");
        boolean hasStart = StringUtils.isNotBlank(start);
        boolean hasEnd = StringUtils.isNotBlank(end);
        if (hasStart || hasEnd) {
            String s = hasStart ? start.trim() : "*";
            String e = hasEnd ? end.trim() : "*";
            return "%s-%s".formatted(s, e);
        }

        return null;
    }

    private String extractPosterFilterQueryFromRequest(HttpServletRequest request) {
        // by default, we don't want to include poster. Only include poster if user
        // choose to include it explicitly.
        String param = request.getParameter("includePoster");
        if (StringUtils.isNotBlank(param)) {
            return String.valueOf(Boolean.parseBoolean(param));
        }
        return "false"; // default is "don't include poster"
    }
}

