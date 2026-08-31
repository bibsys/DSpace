/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.dataprovider;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import com.lyncode.xoai.dataprovider.core.OAIParameters;
import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.data.About;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.internal.ItemHelper;
import com.lyncode.xoai.dataprovider.data.internal.MetadataFormat;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.services.api.DateProvider;
import com.lyncode.xoai.dataprovider.services.api.RepositoryConfiguration;
import com.lyncode.xoai.dataprovider.xml.oaipmh.AboutType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.HeaderType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.MetadataType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.RecordType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.StatusType;
import com.lyncode.xoai.util.XSLPipeline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Build the OAI-PMH record of an item, surviving a broken XSL transformation.
 *
 * <p>The xoai library builds records inside its verb handlers, where a metadata transformation failure --
 * a stylesheet choking on one item's data, or a corrupt compiled document in Solr -- aborts the whole
 * response: the harvester gets a 500 for a page of which one record is rotten, and the harvest stops there.
 * This factory is the single place record dissemination happens for the fault tolerant handlers, and the
 * single place such a failure is contained: the record keeps its regular header, and its metadata carries a
 * {@code <transformationError>} document naming the item and the cause. The failure is logged with its stack
 * trace, the page stays intact, and the harvest goes on.</p>
 */
public class RecordFactory {

    /** Namespace of the error document served in place of metadata that could not be transformed. */
    public static final String ERROR_NAMESPACE = "http://www.dspace.org/xmlns/transformation-error";

    private static final Logger log = LogManager.getLogger(RecordFactory.class);

    private final DateProvider formatter;
    private final XOAIContext context;
    private final RepositoryConfiguration identify;

    /**
     * @param formatter datestamp formatter for record headers
     * @param context   OAI context the records are disseminated in (context transformer, formats, static sets)
     * @param identify  repository configuration, read for the datestamp granularity
     */
    public RecordFactory(DateProvider formatter, XOAIContext context, RepositoryConfiguration identify) {
        this.formatter = formatter;
        this.context = context;
        this.identify = identify;
    }

    /**
     * Build the full OAI-PMH record of an item: header, disseminated metadata, about section.
     *
     * @param parameters the request parameters, read for the metadata prefix to disseminate
     * @param item       the item to disseminate
     * @return the record, whose metadata is an error document if the item could not be transformed
     * @throws CannotDisseminateFormatException if the requested metadata prefix is unknown to the context
     */
    public RecordType createRecord(OAIParameters parameters, Item item) throws CannotDisseminateFormatException {
        MetadataFormat format = context.getFormatByPrefix(parameters.getMetadataPrefix());
        RecordType record = new RecordType();
        HeaderType header = new HeaderType();
        header.setIdentifier(item.getIdentifier());
        ItemHelper itemHelper = new ItemHelper(item);
        header.setDatestamp(formatter.format(item.getDatestamp(), identify.getGranularity()));
        for (ReferenceSet set : itemHelper.getSets(context)) {
            header.getSetSpec().add(set.getSetSpec());
        }
        if (item.isDeleted()) {
            header.setStatus(StatusType.DELETED);
        }
        record.setHeader(header);

        if (!item.isDeleted()) {
            record.setMetadata(disseminate(itemHelper, format, header.getIdentifier()));
            if (item.getAbout() != null) {
                for (About about : item.getAbout()) {
                    AboutType aboutType = new AboutType();
                    aboutType.setAny(about.getXML());
                    record.getAbout().add(aboutType);
                }
            }
        }
        return record;
    }

    private MetadataType disseminate(ItemHelper itemHelper, MetadataFormat format, String identifier) {
        try {
            XSLPipeline pipeline = itemHelper.toPipeline(true);
            if (context.getTransformer().hasXslTemplates()) {
                pipeline.apply(context.getTransformer().getXslTemplates().getValue());
            }
            return new MetadataType(pipeline.apply(format.getXsltTemplates()).getTransformed());
        } catch (WritingXmlException | XMLStreamException | TransformerException | IOException e) {
            log.error("XSL transformation to '{}' failed for item {}; serving an error document as its metadata",
                      format.getPrefix(), identifier, e);
            return new MetadataType(errorDocument(identifier, e));
        }
    }

    /**
     * Build the error document served in place of metadata that could not be transformed.
     *
     * @param identifier OAI identifier of the record the document stands for
     * @param cause      what broke the transformation; its message is embedded, XML-escaped
     * @return a well-formed, namespace qualified XML document
     */
    public static String errorDocument(String identifier, Exception cause) {
        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        return "<transformationError xmlns=\"" + ERROR_NAMESPACE + "\">"
            + "<identifier>" + escape(identifier) + "</identifier>"
            + "<message>" + escape(message) + "</message>"
            + "</transformationError>";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
