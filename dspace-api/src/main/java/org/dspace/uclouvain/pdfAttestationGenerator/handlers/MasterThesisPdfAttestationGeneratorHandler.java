/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.handlers;

import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.configuration.PDFAttestationGeneratorConfiguration;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.PDFGenerationException;
import org.dspace.uclouvain.pdfAttestationGenerator.model.MasterThesisPDFAttestationModel;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;

/** 
* Main handler to generate an attestation for a thesis
*/
public class MasterThesisPdfAttestationGeneratorHandler implements PDFAttestationGeneratorHandler {

    private final Map<String, String> ACCESS_TYPE_LABELS = Map.of(
        "openaccess", "Accès libre",
        "administrator", "Accès interdit",
        "restricted", "Accès restreint UCLouvain",
        "embargo", "Accès embargo",
        "unknown", "Accès inconnu"
    );

    @Autowired
    ItemService itemService;
    @Autowired
    PDFAttestationGeneratorConfiguration config;
    @Autowired
    UCLouvainResourcePolicyService uclouvainResourcePolicyService;

    private String templateName;

    private final String basedir = DSpaceServicesFactory
        .getInstance()
        .getConfigurationService()
        .getProperty("dspace.dir");

    private final String templateDir = DSpaceServicesFactory
        .getInstance()
        .getConfigurationService()
        .getProperty("uclouvain.pdf_attestation.template_dir");

    /** 
    * Recovers data about the object and uses it to construct the PDF file.
    * 
    * @param out The response object from the controller. The PDF file will be passed to it.
    * @param uuid UUID of the targeted DSpace object.
    */
    public void getAttestation(OutputStream out, UUID uuid) throws PDFGenerationException {
        try {
            Context context = new Context();
            File templateFile = new File(basedir + templateDir + this.templateName);

            // Generate the input xml with item data
            String renderedXml = this.generatePDFMasterThesisAttestationFromObjectId(context, uuid);

            // Load rendered data input XML into template
            //1. Inject input data into stream
            InputStream xmlDataInputStream = new ByteArrayInputStream(renderedXml.getBytes());
            StreamSource xmlDataSource = new StreamSource(xmlDataInputStream);

            //2. Instantiate FOP Factory
            FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

            //3. Use transformer to create the final PDF
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(templateFile));
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(xmlDataSource, res);
        } catch (Exception e) {
            throw new PDFGenerationException(e.getMessage());
        }
    };


    /** 
     * From an uuid, generates a PDF attestation for the given object and write it to an InputStream
     * 
     * @param uuid The uuid of the targeted object
     * @return InputStream; The input stream containing the PDF Attestation
     * @throws PDFGenerationException of any exception occurred during PDF generation
     */
    public InputStream getAttestationAsInputStream(UUID uuid) throws PDFGenerationException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            this.getAttestation(out, uuid);
            return convertOutputStreamToInputStream(out);
        } catch (Exception e) {
            throw new PDFGenerationException(e.getMessage());
        }
    }

    // Returns the current template name
    public String getAttestationTemplateName() {
        return this.getTemplateName();
    }

    /** 
    * Generates an XML containing data to feed the template file.
    *
    * @param uuid UUID of the targeted DSpace object.
    * @param context The current DSpace context, used to recover the DSpace object from UUID.
    */
    private String generatePDFMasterThesisAttestationFromObjectId(Context context, UUID uuid) throws SQLException {
        // 1. Retrieve DSpace item's metadata
        Item dspaceItem = itemService.find(context, uuid);
        List<MetadataValue> metadataValues = dspaceItem.getMetadata();
        MasterThesisPDFAttestationModel pdfModel = new MasterThesisPDFAttestationModel();

        HashMap<String, List<String>> map = MetadataUtils.getValuesHashMap(metadataValues);

        // 2. Add information to the model
        pdfModel.title = map.get("dc_title").get(0);

        // Add authors to model
        for (String authorName: map.get("dc_contributor_author")) {
            pdfModel.addAuthor(authorName);
        }
        // Add advisors to model
        for (String advisorName: map.get("dc_contributor_advisor")) {
            pdfModel.addAdvisor(advisorName);
        }
        // Add programs to model
        for (String programName: map.get("masterthesis_degree_code")) {
            pdfModel.addProgram(programName);
        }

        pdfModel.submitter = dspaceItem.getSubmitter().getFullName();

        // Add files to model
        for (Bitstream bitstream: ItemUtils.extractItemFiles(dspaceItem)) {
            pdfModel.addFile(
                bitstream.getName(),
                getFileAccessType(context, bitstream)
            );
        }

        pdfModel.abstractText = map.get("dc_description_abstract").get(0);
        pdfModel.imagePath = "url('file://" + this.basedir + "/assets/images/dial_mem.png" + "')";
        return pdfModel.getRenderedXML();
    }

    /**
     * Extract and translate the access type related to a bitstream depending on restriction policies
     *
     * @param context the application context
     * @param bitstream the bitstream to analyze
     * @return the readable access type corresponding to the bitstream
     */
    private String getFileAccessType(Context context, Bitstream bitstream) {
        try {
            List<ResourcePolicy> policies = uclouvainResourcePolicyService.find(context, bitstream);
            ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(policies);
            String accessStatus = ACCESS_TYPE_LABELS.getOrDefault(masterPolicy.getRpName(), masterPolicy.getRpName());
            if (masterPolicy.getRpName().equals(EMBARGO)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                accessStatus += " -- " + formatter.format(masterPolicy.getStartDate());
            }
            return accessStatus;
        } catch (Exception e) {
            return DefaultAccessStatusHelper.UNKNOWN;
        }
    }

    /** 
     * Utils method to convert a ByteArrayOutputStream to an InputStream
     * 
     * @param out A ByteArrayOutputStream that contains the data to put in the inputStream
     * @return InputStream or null
     * @throws IOException if any IOError occurred
     */
    private static InputStream convertOutputStreamToInputStream(ByteArrayOutputStream out) throws IOException {
        // We use pipes to transfer data from one type of stream to the other
        PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream pipeIn = new PipedInputStream(pipeOut);

        try {
            // Using both Input and Output stream pipes in the same thread isn't recommended (deadlock risks) so we
            // create a new one
            new Thread(() -> {
                try {
                    out.writeTo(pipeOut);
                    pipeOut.close();
                } catch (IOException e) {
                    // do nothing
                }
            }).start();
            return pipeIn;
        } catch (Exception e) {
            return null;
        }
    }

    // GETTERS && SETTERS
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return this.templateName;
    }
}
