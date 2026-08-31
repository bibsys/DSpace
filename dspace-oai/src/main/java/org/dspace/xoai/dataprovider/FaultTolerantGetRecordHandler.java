/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.dataprovider;

import com.lyncode.xoai.dataprovider.core.OAIParameters;
import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.internal.ItemRepositoryHelper;
import com.lyncode.xoai.dataprovider.data.internal.MetadataFormat;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateRecordException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.services.api.DateProvider;
import com.lyncode.xoai.dataprovider.xml.oaipmh.GetRecordType;

/**
 * GetRecord handler whose record is built by {@link RecordFactory}: an item whose XSL transformation fails
 * is served with an error document as its metadata instead of a 500, so the failure can be seen from the
 * OAI output itself.
 *
 * <p>Copied from {@code com.lyncode.xoai.dataprovider.handlers.GetRecordHandler} (xoai 3.4.0), where record
 * creation is inlined and cannot be overridden; the only functional change is the delegation to the
 * factory.</p>
 */
public class FaultTolerantGetRecordHandler extends VerbHandler<GetRecordType> {

    private final XOAIContext context;
    private final ItemRepositoryHelper itemRepositoryHelper;
    private final RecordFactory recordFactory;

    /**
     * @param formatter            datestamp formatter
     * @param context              OAI context of the request
     * @param itemRepositoryHelper item repository the record is read from
     * @param recordFactory        builds the record, containing transformation failures
     */
    public FaultTolerantGetRecordHandler(DateProvider formatter, XOAIContext context,
                                         ItemRepositoryHelper itemRepositoryHelper, RecordFactory recordFactory) {
        super(formatter);
        this.context = context;
        this.itemRepositoryHelper = itemRepositoryHelper;
        this.recordFactory = recordFactory;
    }

    @Override
    public GetRecordType handle(OAIParameters parameters) throws OAIException, HandlerException {
        MetadataFormat format = context.getFormatByPrefix(parameters.getMetadataPrefix());
        Item item = itemRepositoryHelper.getItem(parameters.getIdentifier());
        if (!context.isItemShown(item)) {
            throw new IdDoesNotExistException("ContextConfiguration ignores this item");
        }
        if (!format.isApplicable(item)) {
            throw new CannotDisseminateRecordException("FormatConfiguration not applicable to this item");
        }
        GetRecordType result = new GetRecordType();
        result.setRecord(recordFactory.createRecord(parameters, item));
        return result;
    }
}
