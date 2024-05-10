package org.dspace.uclouvain.rest;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.Hasher;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.web.ContextUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uclouvain/bitstream")
public class BitstreamDownloadRestController {

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService(); 
    private String algo = this.configurationService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5");
    private String encryptionKey = this.configurationService.getProperty("uclouvain.api.bitstream.download.secret", "");
    private Integer bitstreamContentBufferSize = Integer.parseInt(this.configurationService.getProperty("uclouvain.api.bitstream.download.buffer.size", "10240"));
    private String promoterFieldName = this.configurationService.getProperty("uclouvain.api.bitstream.download.promoterfield", "advisors.email");
    private Logger logger = LogManager.getLogger(BitstreamDownloadRestController.class);
    private MetadataField promoterField;

    @Autowired
    private BitstreamService bitstreamService;

    public BitstreamDownloadRestController() {
        try {
            this.promoterField = new MetadataField(this.promoterFieldName);
        } catch (Exception e) {
            logger.error("Error while instantiating the MetadataField", e);
            this.promoterField = null;
        }
    }

    /**
     * Main entry point to download a bitstream. The bitstream is identified by its UUID.
     * First, we retrieve the bitstream with the given UUID and the given hash parameter.
     * We then check if the hash correspond to one of the promoters of the item containing the bitstream.
     * @param uuid: The UUID of the bitstream to download.
     * @param response: The response object to stream the bitstream to.
     * @param request: The request object to retrieve the hash parameter.
     * @return: :: IF the bitstream is found and the hash is correct, the bitstream is streamed to the response output stream and a 200 code response is returned.
     *          :: IF the bitstream is not found, a 404 error is returned.
     *          :: IF the hash is missing, a 400 error is returned.
     * @throws SQLException
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}/content")
    public ResponseEntity getBitstreamContent(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Bitstream bitstream = this.bitstreamService.find(context, uuid);
        String hash = request.getParameter("hash");

        if (bitstream == null) return new ResponseEntity<String>("Bitstream not found", HttpStatus.NOT_FOUND);
        if (hash == null || isEmpty(hash)) return new ResponseEntity<String>("Missing hash parameter", HttpStatus.BAD_REQUEST);
        
        if (this.isAuthorized(context, bitstream, hash)){
            try {
                String mimeType = bitstream.getFormat(context).getMIMEType();
                // Using the 'ContentDisposition' of  Spring.http which helps to build the header. It also sanitize the filename.
                ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(bitstream.getName()).build();

                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());;
                response.setContentLength((int) bitstream.getSizeBytes());
                response.setContentType(mimeType);

                context.turnOffAuthorisationSystem();
                // Fixed buffer size to minimize memory usage. If we have big files it will save us by not loading everything at once in the memory.
                byte[] buffer = new byte[this.bitstreamContentBufferSize];

                InputStream input = bitstreamService.retrieve(context, bitstream);
                OutputStream output = response.getOutputStream();

                for (int length = 0; (length = input.read(buffer)) > 0;){
                    output.write(buffer, 0, length);
                }

                response.flushBuffer();
                context.restoreAuthSystemState();
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                this.logger.error("Could not retrieve bitstream's input stream: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            this.logger.warn("Failed bitstream download attempt; bitstream uuid: " + uuid + "; used hash: " + (hash != null ? hash : "NONE"));
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /** 
     * First retrieve the list of promoters from the item containing the bitstream.
     * Then hash their email addresses and compare the hash with the one given with the request.
     * @param ctx: The current DSpace context.
     * @param bitstream: The bitstream to check the authorization for.
     * @param hash: The hash to compare with the promoters' hashes.
     * @return: If one hash match the given one, return true, else return false..
    */
    private boolean isAuthorized(Context ctx, Bitstream bitstream, String hash) {
        try {
            DSpaceObject parentObject = this.bitstreamService.getParentObject(ctx, bitstream);
            
            if (parentObject != null && parentObject instanceof Item) {
                Item dspaceItem = (Item) parentObject;
                List<String> hashList = this.getPromotersEmailsAsHash(ctx, dspaceItem);

                return !hashList.isEmpty() && hashList.contains(hash);
            }
            return false;
        } catch (Exception e) {
            logger.error("Unhandled exception occurred while checking bitstream access authorization", e);
            return false;
        }
    }

    /**
     * Retrieve the promoters email list from the item containing the bitstream and hash them using the encryption key.
     * @param ctx: The current DSpace context.
     * @param item: The item which is the parent of the bitstream.
     * @return: A list of hashed email addresses of the promoters of the item.
     */
    private List<String> getPromotersEmailsAsHash(Context ctx, Item item) {
        if (this.encryptionKey.isEmpty()){
            logger.error("!! NO ENCRYPTION KEY PROVIDED FOR BITSTREAM PROMOTER HASHING !!");
            return new ArrayList<String>();
        }
        if (this.promoterField == null){
            logger.error("Cannot retrieve promoters because `this.promoterField` is null.");
            return new ArrayList<String>();
        }

        try {
            Hasher hasher = new Hasher(this.algo, this.encryptionKey);
            ItemService currentItemService = item.getItemService();

            return currentItemService.getMetadata(
                item, 
                this.promoterField.getSchema(),
                this.promoterField.getElement(),
                this.promoterField.getQualifier(),
                null
            ).stream().map(md -> md.getValue()).map(value -> new String(hasher.processHashAsString(value))).collect(Collectors.toList());
        } catch (NoSuchAlgorithmException e) {
            logger.warn("'" + algo + "' is not a known algorithm name.", e);
            return new ArrayList<String>();
        }
    }
}