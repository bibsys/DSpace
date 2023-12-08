/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.configuration;

import java.io.File;
import java.io.FileNotFoundException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.model.Handler;
import org.dspace.uclouvain.pdfAttestationGenerator.model.Handlers;
import org.springframework.beans.factory.annotation.Autowired;

public class PDFAttestationGeneratorConfiguration {

    private Handlers handlersConfiguration;

    @Autowired
    private String source = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");

    /** 
     * Loads the xml configuration file in a java object
     * 
     * @param xmlConfigPath The path to the configuration file starting from "dspace.dir"
     */
    public PDFAttestationGeneratorConfiguration(String xmlConfigPath) throws FileNotFoundException {
        XStream xStream = new XStream();
        File xmlConfig = new File(this.source + xmlConfigPath);
        if (xmlConfig.exists()) {
            // Adding permissions to allow access to classes
            xStream.addPermission(NoTypePermission.NONE);
            xStream.addPermission(NullPermission.NULL);
            xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
            xStream.allowTypesByWildcard(new String[] {
                "org.dspace.uclouvain.pdfAttestationGenerator.model.Handler",
                "org.dspace.uclouvain.pdfAttestationGenerator.model.Handlers",
            });
            xStream.processAnnotations(Handlers.class);
            this.handlersConfiguration = (Handlers)xStream.fromXML(xmlConfig);
        } else {
            throw new FileNotFoundException("Could not find the PDF attestation generator configuration file");
        }
    }

    /** 
     * Search for a specific configuration based on the itemType
     * 
     * @param itemType The type of the item to search for the config
     * @return Handler or null if not found
     */
    public Handler getConfigForItemType(String itemType) {
        for (Handler handler: this.handlersConfiguration.handlers) {
            if (handler.itemType.equals(itemType)) {
                return handler;
            }
        }
        return null;
    }
}