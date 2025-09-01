/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.services.DirectLinkService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main controller for the bitstream's download URL API.
 * This controller is used to download a bitstream from a URL and using a Hash.
 * This hash must correspond to one of the promoters or managers of the item containing the bitstream.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@RestController
@RequestMapping("/api/uclouvain/bitstream")
public class BitstreamDirectDownloadRestController {

    private final Logger logger = LogManager.getLogger(BitstreamDirectDownloadRestController.class);

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final int bufferSize = configService.getIntProperty("uclouvain.api.bitstream.download.buffer.size", 10240);

    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private DirectLinkService uclouvainDirectLinkService;

    /**
     * Main entry point to download a bitstream. The bitstream is identified by its UUID.
     *
     * @param uuid The UUID of the bitstream to download.
     * @param response The response object to stream the bitstream to.
     * @param request The request object to retrieve the hash parameter.
     * @return * HTTP-200: If the bitstream is found and the hash is correct, the bitstream is streamed to the response
     *                     output stream.
     *         * HTTP-400: for missing parameter (hash)
     *         * HTTP-401: if current user isn't authorized to download the bitstream content
     *         * HTTP-404: if the bitstream is not found.
     *         * HTTP-500: if other problems occurred
     * @throws SQLException for any database exception
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}/content")
    public ResponseEntity getBitstreamContent(
            @PathVariable UUID uuid,
            @RequestParam(name = "hash") String hash,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Bitstream bitstream = bitstreamService.find(context, uuid);
        if (bitstream == null) {
            return new ResponseEntity<>("Bitstream not found", HttpStatus.NOT_FOUND);
        }
        if (isAuthorized(context, bitstream, hash)) {
            try {
                String mimeType = bitstream.getFormat(context).getMIMEType();
                // Using the 'ContentDisposition' of Spring.http which helps to build the header.
                // It also sanitizes the filename.
                ContentDisposition contentDisposition = ContentDisposition
                        .builder("attachment")
                        .filename(bitstream.getName())
                        .build();
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());;
                response.setContentLength((int) bitstream.getSizeBytes());
                response.setContentType(mimeType);

                context.turnOffAuthorisationSystem();
                // Fixed buffer size to minimize memory usage. To prevent loading all file content in memory at once
                byte[] buffer = new byte[this.bufferSize];
                InputStream input = bitstreamService.retrieve(context, bitstream);
                OutputStream output = response.getOutputStream();
                for (int length = 0; (length = input.read(buffer)) > 0;) {
                    output.write(buffer, 0, length);
                }
                response.flushBuffer();
                context.restoreAuthSystemState();
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (IOException | AuthorizeException e) {
                logger.error("Could not retrieve bitstream's input stream: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            logger.warn("Failed bitstream download attempt; bitstream=[" + uuid + "] with hash=[" +  hash + "]");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Validate the token received from the URL
     * @param context The current DSpace context.
     * @param bitstream The bitstream to check the authorization for.
     * @param hash The token/secret to validate
     * @return True if the token/secret is valid, False otherwise
     */
    private boolean isAuthorized(Context context, Bitstream bitstream, String hash) {
        try {
            return uclouvainDirectLinkService.validateToken(context, bitstream, hash);
        } catch (Exception e) {
            logger.info("Token validation exception :: " + e.getMessage(), e);
            return false;
        }
    }
}