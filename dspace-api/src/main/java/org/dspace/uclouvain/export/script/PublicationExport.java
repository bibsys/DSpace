/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.script;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.uclouvain.export.result.ExportResult;
import org.dspace.uclouvain.export.services.UCLouvainExportService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.utils.DSpace;

/**
 * Custom publication export script to export publication of a profile in FWB or FNRS format.
 * This is used when running export process in the frontend (asynchronous process).
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class PublicationExport extends DSpaceRunnable<PublicationExportScriptConfiguration<PublicationExport>> {

    public enum PublicationExportType {
        FNRS,
        FWB
    }

    private UCLouvainExportService exportService;

    private String uuid;
    private String fgs;
    private PublicationExportType exportType;
    private String startYear;
    private String endYear;
    private String includePosters = "false";

    @Override
    public void setup() throws ParseException {
        exportService = UCLouvainServiceFactory.getInstance().getExportService();

        uuid = commandLine.getOptionValue('u');
        fgs = commandLine.getOptionValue('f');
        String type = commandLine.getOptionValue('t');
        startYear = commandLine.getOptionValue('s');
        endYear = commandLine.getOptionValue('e');

        if (StringUtils.isNotBlank(commandLine.getOptionValue('i'))) {
            includePosters = String.valueOf(Boolean.parseBoolean(commandLine.getOptionValue('i')));
        }

        if (StringUtils.isAllBlank(uuid, fgs)) {
            throw new ParseException("Either uuid or fgs should be provided");
        }

        if (StringUtils.isBlank(type)) {
            throw new ParseException("The export type (-t) is required.");
        }

        try {
            // Check that the provided value is accepted.
            exportType = PublicationExportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                "Illegal value for --type : " + type + ". Accepted values are "
                + Arrays.toString(PublicationExportType.values()));
        }
    }

    @Override
    public void internalRun() throws Exception {
        Context context = new Context(Context.Mode.READ_ONLY);
        Map<String, String> filters = parseFilters();

        try {
            handler.logInfo(
                "Starting export process for ids: 'fgs': %s, 'uuid': %s; with filters: %s".formatted(
                    fgs,
                    uuid,
                    filters
                )
            );
            ExportResult exportResult = export(context, uuid, fgs, filters, exportType);
            handler.logInfo("Export ended, streaming content to user...");
            // READ_WRITE mode is necessary for writing/reading files in dspace.
            context.setMode(Context.Mode.READ_WRITE);
            handler.writeFilestream(
                context,
                exportResult.getFileName(),
                exportResult.readStream(),
                exportResult.getMimeType()
            );
            context.complete();
            handler.logInfo("Export done.");
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PublicationExportScriptConfiguration<PublicationExport> getScriptConfiguration() {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        return serviceManager.getServiceByName("publication-export", PublicationExportScriptConfiguration.class);
    }

    /**
     * Parse filters based on provided script configuration.
     */
    private Map<String, String> parseFilters() {
        Map<String, String> filters = new HashMap<>();
        addIfNotBlank(filters, "year", extractYear());
        addIfNotBlank(filters, "includePoster", includePosters);
        return filters;
    }

    private void addIfNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    /**
     * Extract the year from the script configuration. Year filter is built by concatenating the start and end year.
     * 
     * @return A filter string for the year.
     */
    private String extractYear() {
        boolean hasStart = StringUtils.isNotBlank(startYear);
        boolean hasEnd = StringUtils.isNotBlank(endYear);
        if (hasStart || hasEnd) {
            String s = hasStart ? startYear.trim() : "*";
            String e = hasEnd ? endYear.trim() : "*";
            return "%s-%s".formatted(s, e);
        }
        return null;
    }

    /**
     * Using the provided configuration, do the correct export and return an {@link ExportResult} object.
     * 
     * @param context The current DSace application context.
     * @param uuid The uuid of the profile to export publications of.
     * @param fgs The fgs of the profile to export publications of.
     * @param params The export params.
     * @param exportType The type of export that needs to be performed (fnrs, fwb...).
     * @return An {@link ExportResult} object containing all export information (file content, filename, MIME...)
     */
    private ExportResult export(
        Context context, String uuid, String fgs, Map<String, String> params, PublicationExportType exportType
    ) throws SearchServiceException, CrosswalkException {
        handler.logInfo("Export type is '%s'".formatted(exportType));
        return switch (exportType) {
            case FNRS -> exportService.getAuthorFNRSBibliography(context, uuid, fgs, params);
            case FWB  -> exportService.getAuthorFWBBibliography(context, uuid, fgs, params);
        };
    }
}
