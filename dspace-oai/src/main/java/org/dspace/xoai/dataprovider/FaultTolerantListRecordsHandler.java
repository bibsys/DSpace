/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.dataprovider;

import java.util.List;

import com.lyncode.xoai.dataprovider.core.ListItemsResults;
import com.lyncode.xoai.dataprovider.core.OAIParameters;
import com.lyncode.xoai.dataprovider.core.ResumptionToken;
import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.internal.ItemRepositoryHelper;
import com.lyncode.xoai.dataprovider.data.internal.SetRepositoryHelper;
import com.lyncode.xoai.dataprovider.exceptions.DoesNotSupportSetsException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.NoMatchesException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.services.api.DateProvider;
import com.lyncode.xoai.dataprovider.services.api.ResumptionTokenFormatter;
import com.lyncode.xoai.dataprovider.xml.oaipmh.ListRecordsType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.ResumptionTokenType;

/**
 * ListRecords handler whose records are built by {@link RecordFactory}: one record whose XSL transformation
 * fails is served with an error document as its metadata instead of failing the whole page with a 500.
 *
 * <p>Copied from {@code com.lyncode.xoai.dataprovider.handlers.ListRecordsHandler} (xoai 3.4.0), where record
 * creation is private and cannot be overridden; the only functional change is the delegation to the
 * factory.</p>
 */
public class FaultTolerantListRecordsHandler extends VerbHandler<ListRecordsType> {

    private final int maxListSize;
    private final SetRepositoryHelper setRepository;
    private final ItemRepositoryHelper itemRepositoryHelper;
    private final XOAIContext context;
    private final ResumptionTokenFormatter resumptionFormat;
    private final RecordFactory recordFactory;

    /**
     * @param formatter            datestamp formatter
     * @param maxListSize          page size of the verb
     * @param setRepository        set repository, asked whether sets are supported
     * @param itemRepositoryHelper item repository the page is read from
     * @param context              OAI context of the request
     * @param resumptionFormat     resumption token codec
     * @param recordFactory        builds each record, containing transformation failures
     */
    public FaultTolerantListRecordsHandler(DateProvider formatter, int maxListSize,
                                           SetRepositoryHelper setRepository,
                                           ItemRepositoryHelper itemRepositoryHelper,
                                           XOAIContext context, ResumptionTokenFormatter resumptionFormat,
                                           RecordFactory recordFactory) {
        super(formatter);
        this.maxListSize = maxListSize;
        this.setRepository = setRepository;
        this.itemRepositoryHelper = itemRepositoryHelper;
        this.context = context;
        this.resumptionFormat = resumptionFormat;
        this.recordFactory = recordFactory;
    }

    @Override
    public ListRecordsType handle(OAIParameters parameters) throws OAIException, HandlerException {
        ListRecordsType res = new ListRecordsType();
        ResumptionToken token = parameters.getResumptionToken();
        int length = maxListSize;

        if (parameters.hasSet() && !setRepository.supportSets()) {
            throw new DoesNotSupportSetsException();
        }

        ListItemsResults result;
        if (!parameters.hasSet()) {
            if (parameters.hasFrom() && !parameters.hasUntil()) {
                result = itemRepositoryHelper.getItems(context, token.getOffset(), length,
                                                       parameters.getMetadataPrefix(), parameters.getFrom());
            } else if (!parameters.hasFrom() && parameters.hasUntil()) {
                result = itemRepositoryHelper.getItemsUntil(context, token.getOffset(), length,
                                                            parameters.getMetadataPrefix(), parameters.getUntil());
            } else if (parameters.hasFrom() && parameters.hasUntil()) {
                result = itemRepositoryHelper.getItems(context, token.getOffset(), length,
                                                       parameters.getMetadataPrefix(), parameters.getFrom(),
                                                       parameters.getUntil());
            } else {
                result = itemRepositoryHelper.getItems(context, token.getOffset(), length,
                                                       parameters.getMetadataPrefix());
            }
        } else {
            if (!setRepository.exists(context, parameters.getSet())) {
                throw new NoMatchesException();
            }
            if (parameters.hasFrom() && !parameters.hasUntil()) {
                result = itemRepositoryHelper.getItems(context, token.getOffset(), length,
                                                       parameters.getMetadataPrefix(), parameters.getSet(),
                                                       parameters.getFrom());
            } else if (!parameters.hasFrom() && parameters.hasUntil()) {
                result = itemRepositoryHelper.getItemsUntil(context, token.getOffset(), length,
                                                            parameters.getMetadataPrefix(), parameters.getSet(),
                                                            parameters.getUntil());
            } else if (parameters.hasFrom() && parameters.hasUntil()) {
                result = itemRepositoryHelper.getItems(context, token.getOffset(), length,
                                                       parameters.getMetadataPrefix(), parameters.getSet(),
                                                       parameters.getFrom(), parameters.getUntil());
            } else {
                result = itemRepositoryHelper.getItems(context, token.getOffset(), length,
                                                       parameters.getMetadataPrefix(), parameters.getSet());
            }
        }

        List<Item> results = result.getResults();
        if (results.isEmpty()) {
            throw new NoMatchesException();
        }

        ResumptionToken newToken;
        if (result.hasMore()) {
            newToken = new ResumptionToken(token.getOffset() + length, parameters);
        } else {
            newToken = new ResumptionToken();
        }

        if (parameters.hasResumptionToken() || !newToken.isEmpty()) {
            ResumptionTokenType resToken = new ResumptionTokenType();
            if (!newToken.isEmpty()) {
                resToken.setValue(resumptionFormat.format(newToken));
            }
            resToken.setCursor(token.getOffset() / maxListSize);
            if (result.hasTotalResults()) {
                resToken.setCompleteListSize(result.getTotal());
            }
            res.setResumptionToken(resToken);
        }

        for (Item item : results) {
            res.getRecord().add(recordFactory.createRecord(parameters, item));
        }

        return res;
    }
}
