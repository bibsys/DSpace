/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.dataprovider;

import java.io.OutputStream;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;

import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.dataprovider.core.OAIParameters;
import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.data.internal.ItemRepositoryHelper;
import com.lyncode.xoai.dataprovider.data.internal.SetRepositoryHelper;
import com.lyncode.xoai.dataprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.InvalidContextException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.handlers.ErrorHandler;
import com.lyncode.xoai.dataprovider.handlers.IdentifyHandler;
import com.lyncode.xoai.dataprovider.handlers.ListIdentifiersHandler;
import com.lyncode.xoai.dataprovider.handlers.ListMetadataFormatsHandler;
import com.lyncode.xoai.dataprovider.handlers.ListSetsHandler;
import com.lyncode.xoai.dataprovider.services.api.DateProvider;
import com.lyncode.xoai.dataprovider.services.api.ItemRepository;
import com.lyncode.xoai.dataprovider.services.api.RepositoryConfiguration;
import com.lyncode.xoai.dataprovider.services.api.ResumptionTokenFormatter;
import com.lyncode.xoai.dataprovider.services.api.SetRepository;
import com.lyncode.xoai.dataprovider.services.impl.BaseDateProvider;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMH;
import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMHtype;
import com.lyncode.xoai.dataprovider.xml.oaipmh.RequestType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.VerbType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OAI-PMH data provider whose GetRecord and ListRecords verbs are handled by the fault tolerant handlers:
 * a record whose XSL transformation fails is served with an error document as its metadata (and the failure
 * logged) instead of turning the whole response into a 500 and killing the harvest.
 *
 * <p>Copied from {@code com.lyncode.xoai.dataprovider.OAIDataProvider} (xoai 3.4.0), which hardwires its
 * verb handlers; the only functional change is which handlers serve GetRecord and ListRecords. The other
 * verbs never disseminate metadata and keep the library handlers.</p>
 */
public class FaultTolerantOAIDataProvider {
    private static final Logger log = LogManager.getLogger(FaultTolerantOAIDataProvider.class);
    private static final DateProvider FORMATTER = new BaseDateProvider();

    private final XOAIManager manager;
    private final RepositoryConfiguration repositoryConfiguration;
    private final ResumptionTokenFormatter resumptionTokenFormatter;

    private final FaultTolerantGetRecordHandler getRecordHandler;
    private final IdentifyHandler identifyHandler;
    private final ListIdentifiersHandler listIdentifiersHandler;
    private final ListMetadataFormatsHandler listMetadataFormatsHandler;
    private final FaultTolerantListRecordsHandler listRecordsHandler;
    private final ListSetsHandler listSetsHandler;
    private final ErrorHandler errorHandler = new ErrorHandler();

    /**
     * @param manager        xoai manager holding the configured contexts and page sizes
     * @param contextUrl     base url of the requested OAI context
     * @param identify       repository configuration
     * @param setRepository  set repository
     * @param itemRepository item repository
     * @param format         resumption token codec
     * @throws InvalidContextException if no context is configured for the given base url
     */
    public FaultTolerantOAIDataProvider(XOAIManager manager, String contextUrl, RepositoryConfiguration identify,
                                        SetRepository setRepository, ItemRepository itemRepository,
                                        ResumptionTokenFormatter format) throws InvalidContextException {
        this.manager = manager;
        XOAIContext xoaiContext = manager.getContextManager().getOAIContext(contextUrl);
        if (xoaiContext == null) {
            throw new InvalidContextException("ContextConfiguration \"" + contextUrl + "\" does not exist");
        }
        this.repositoryConfiguration = identify;
        this.resumptionTokenFormatter = format;

        SetRepositoryHelper setRepositoryHelper = new SetRepositoryHelper(setRepository);
        ItemRepositoryHelper itemRepositoryHelper = new ItemRepositoryHelper(itemRepository);
        // This is our custom factory !
        // This factory is able to provide fault-tolerant handler
        RecordFactory recordFactory = new RecordFactory(FORMATTER, xoaiContext, identify);

        getRecordHandler = new FaultTolerantGetRecordHandler(
            FORMATTER,
            xoaiContext,
            itemRepositoryHelper,
            recordFactory
        );
        identifyHandler = new IdentifyHandler(
            FORMATTER,
            identify,
            new ArrayList<>()
        );
        listMetadataFormatsHandler = new ListMetadataFormatsHandler(
            FORMATTER,
            itemRepositoryHelper,
            xoaiContext
        );
        listRecordsHandler = new FaultTolerantListRecordsHandler(
            FORMATTER,
            manager.getMaxListRecordsSize(),
            setRepositoryHelper,
            itemRepositoryHelper,
            xoaiContext,
            format,
            recordFactory
        );
        listIdentifiersHandler = new ListIdentifiersHandler(
            FORMATTER,
            manager.getMaxListIdentifiersSize(),
            setRepositoryHelper,
            itemRepositoryHelper,
            identify,
            xoaiContext,
            format
        );
        listSetsHandler = new ListSetsHandler(
            FORMATTER,
            manager.getMaxListSetsSize(),
            setRepositoryHelper,
            xoaiContext,
            format
        );
    }

    /**
     * Serve one OAI-PMH request as a response object.
     *
     * @param params the raw request parameters
     * @return the response, carrying either the verb result or the OAI-PMH error the request maps to
     * @throws OAIException on repository-side failures
     */
    public OAIPMH handle(OAIRequestParameters params) throws OAIException {
        OAIPMH response = new OAIPMH(manager);
        OAIPMHtype info = new OAIPMHtype();
        response.setInfo(info);

        RequestType request = new RequestType();
        info.setRequest(request);
        info.setResponseDate(FORMATTER.now());

        request.setValue(repositoryConfiguration.getBaseUrl());
        try {
            OAIParameters parameters = new OAIParameters(params, resumptionTokenFormatter);
            VerbType verb = parameters.getVerb();
            request.setVerb(verb);

            if (params.getResumptionToken() != null) {
                request.setResumptionToken(params.getResumptionToken());
            }
            if (params.getIdentifier() != null) {
                request.setIdentifier(parameters.getIdentifier());
            }
            if (params.getFrom() != null) {
                try {
                    request.setFrom(FORMATTER.parse(params.getFrom()));
                } catch (java.text.ParseException e) {
                    throw new BadArgumentException("Invalid date given in from parameter");
                }
            }
            if (params.getMetadataPrefix() != null) {
                request.setMetadataPrefix(params.getMetadataPrefix());
            }
            if (params.getSet() != null) {
                request.setSet(params.getSet());
            }
            if (params.getUntil() != null) {
                try {
                    request.setUntil(FORMATTER.parse(params.getUntil()));
                } catch (java.text.ParseException e) {
                    throw new BadArgumentException("Invalid date given in until parameter");
                }
            }

            switch (verb) {
                case IDENTIFY:
                    info.setIdentify(identifyHandler.handle(parameters));
                    break;
                case LIST_SETS:
                    info.setListSets(listSetsHandler.handle(parameters));
                    break;
                case LIST_METADATA_FORMATS:
                    info.setListMetadataFormats(listMetadataFormatsHandler.handle(parameters));
                    break;
                case GET_RECORD:
                    info.setGetRecord(getRecordHandler.handle(parameters));
                    break;
                case LIST_IDENTIFIERS:
                    info.setListIdentifiers(listIdentifiersHandler.handle(parameters));
                    break;
                case LIST_RECORDS:
                    info.setListRecords(listRecordsHandler.handle(parameters));
                    break;
                default:
                    break;
            }
        } catch (HandlerException e) {
            log.debug(e.getMessage(), e);
            info.getError().add(errorHandler.handle(e));
        }

        return response;
    }

    /**
     * Serve one OAI-PMH request onto an output stream.
     *
     * @param params the raw request parameters
     * @param out    where the XML response is written
     * @throws OAIException        on repository-side failures
     * @throws XMLStreamException  if the response cannot be written as XML
     * @throws WritingXmlException if the response cannot be written as XML
     */
    public void handle(OAIRequestParameters params, OutputStream out)
            throws OAIException, XMLStreamException, WritingXmlException {
        XmlOutputContext context = XmlOutputContext.emptyContext(out);
        context.getWriter().writeStartDocument();
        this.handle(params).write(context);
        context.getWriter().writeEndDocument();
        context.getWriter().flush();
        context.getWriter().close();
    }
}
