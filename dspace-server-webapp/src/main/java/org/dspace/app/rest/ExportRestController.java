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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.utils.HttpHeadersInitializer;
import org.dspace.core.Context;
import org.dspace.uclouvain.export.result.ExportResult;
import org.dspace.uclouvain.export.services.UCLouvainExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/uclouvain/export")
public class ExportRestController {

    @Autowired
    UCLouvainExportService uclouvainExportService;

    private final Logger logger = LogManager.getLogger(ExportRestController.class);

    // MAIN ENDPOINTS --------------------------------------------------------------------------------------------------

    @GetMapping(value = "/custom")
    public ResponseEntity<StreamingResponseBody> customExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "style", required = true) String style,
        @RequestParam(value = "format", required = true) String format,
        @RequestParam(value = "query", required = true) String query
    ) throws Exception {
        ExportResult result = uclouvainExportService.getExportResult(context, style, format, query);
        return parseResponse(result, request, response);
    }

    @GetMapping(value = "/fwb")
    public ResponseEntity<StreamingResponseBody> fwbExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "authorFGS", required = false) String authorFGS,
        @RequestParam(value = "authorUUID", required = false) String authorUUID
    ) throws Exception {
        if (authorFGS == null && authorUUID == null) {
            throw new DSpaceBadRequestException("No author identifiers were provided.");
        }
        ExportResult result = uclouvainExportService.getAuthorFWBBibliography(context, authorUUID, authorFGS);
        return parseResponse(result, request, response);
    }

    @GetMapping(value = "/fnrs")
    public ResponseEntity<StreamingResponseBody> fnrsExport(
        Context context, HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "authorFGS", required = false) String authorFGS,
        @RequestParam(value = "authorUUID", required = false) String authorUUID
    ) throws Exception {
        if (authorFGS == null && authorUUID == null) {
            throw new DSpaceBadRequestException("No author identifiers were provided.");
        }
        ExportResult result = uclouvainExportService.getAuthorFNRSBibliography(context, authorUUID, authorFGS);
        return parseResponse(result, request, response);
    }

    // PRIVATE METHODS -------------------------------------------------------------------------------------------------

    /**
     * Parse the export result into a ResponseBody that can be streamed to the final user.
     * @param result The export result containing export data.
     * @param request The request object to generate the headers.
     * @param response The response object to generate the headers.
     * @return A full response entity that can be send to the final user?
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
     * Generate headers for a given export result. Set correcte content-length, content-type and content-disposition.
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
        if (headersInitializer.isValid()) {
            return headersInitializer.initialiseHeaders();
        } else {
            throw new IOException("Non-valid export headers.");
        }
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
}

