/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import de.undercouch.citeproc.CSL;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.content.integration.crosswalks.ReferCrosswalk;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.integration.crosswalks.model.TemplateLine;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Implementation of {@link ItemExportCrosswalk} to produce an output from an Item starting from a template with some
 * UCLouvain specific changes to {@link ReferCrosswalk}
 * Additional specific template tags are :
 *   - @item.citation@ : the citation corresponding to the item (format & style are defined by class attributes)
 *   - @item.handle@ : the full item handle URL
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainReferCrosswalk extends ReferCrosswalk {

    protected static final Logger log = LogManager.getLogger(UCLouvainReferCrosswalk.class);

    // CLASS ATTRIBUTES ================================================================================================
    @Autowired
    @Qualifier("DSpaceListItemDataProvider")
    protected ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;
    @Autowired
    private HandleService handleService;

    private String citationStyle;
    private String style;
    private String format = "text";

    // OVERRIDE METHODS ================================================================================================
    @Override
    protected List<String> getMetadataValuesForLine(Context context, TemplateLine line, Item item) {
        if (line.getField().equals("item.citation")) {
            return List.of(getCitationForItem(item));
        }
        if (line.getField().equals("item.handle")) {
            try {
                return List.of(handleService.resolveToURL(context, item.getHandle()));
            } catch (SQLException sqle) {
                log.warn("Unable to get handle URL for {}", item.getID(), sqle);
                return Collections.emptyList();
            }
        }
        return super.getMetadataValuesForLine(context, line, item);
    }

    // CLASS METHODS ===================================================================================================

    protected DSpaceListItemDataProvider getDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }

    /**
     * Generate a citation for an item.
     * Citation style & format are defined by class attributes (during bean instantiation)
     * @param item the item to cite
     * @return the corresponding citation for the item. Always return a String, never null or throw an exception !!!
     *         If any citation generation error occurred, an error message will be returned.
     */
    private String getCitationForItem(Item item) {
        try {
            DSpaceListItemDataProvider provider = getDataProviderInstance();
            provider.processItem(item);
            String id = provider.getIds()
                .stream()
                .findFirst()
                .orElseThrow(() -> new CitationGenerationException(citationStyle, item));
            return CSL.makeAdhocBibliography(style, format, provider.retrieveItem(id)).makeString();
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            return ex.getMessage();
        }
    }

    /**
     * Load a citation style first from custom citation file ; if not found, try to use predefined CSL style
     * @param style the citation file or predefined citation style
     * @throws IOException if the custom citation file cannot be found AND predefined citation style is not supported
     */
    private void loadStyle(String style) throws IOException {
        try {
            this.style = readXmlStyleContent(style);
        } catch (IOException ioe) {
            log.warn("Error loading style !!! Try to load from CSL library");
            if (CSL.supportsStyle(style)) {
                this.style = style;
            } else {
                throw ioe;
            }
        }
    }

    /**
     * Load custom citation style from configuration file
     * @param style the citation style name
     * @return Content of the XML citation file
     * @throws IOException if the file cannot be found
     */
    private String readXmlStyleContent(String style) throws IOException {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File styleFile = new File(parent, style);
        if (!styleFile.exists()) {
            parent = parent + File.separator + "crosswalks" + File.separator + "csl";
            styleFile = new File(parent, style);
            if (!styleFile.exists()) {
                throw new FileNotFoundException("Could not find style " + style);
            }
        }
        try (FileInputStream fis = new FileInputStream(styleFile)) {
            return IOUtils.toString(fis, Charset.defaultCharset());
        }
    }

    // GETTER & SETTER =================================================================================================
    public String getCitationStyle() {
        return citationStyle;
    }
    public void setCitationStyle(String citationStyle) {
        this.citationStyle = citationStyle;
        try {
            loadStyle(citationStyle);
        } catch (IOException ioe) {
            log.error("Error reading CSL style \"{}\" :: {}", citationStyle, ioe);
            throw new BeanInitializationException("Error reading CSL style \"" + citationStyle + "\"", ioe);
        }
    }

    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
}
