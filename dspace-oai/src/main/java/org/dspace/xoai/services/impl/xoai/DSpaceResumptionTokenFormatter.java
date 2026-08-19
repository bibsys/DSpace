/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import com.lyncode.xoai.dataprovider.core.ResumptionToken;
import com.lyncode.xoai.dataprovider.exceptions.BadResumptionToken;
import com.lyncode.xoai.dataprovider.services.api.ResumptionTokenFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.util.DateUtils;


/**
 * Decode and encode the resumptionToken values DSpace hands out.
 *
 * <p>A token is {@code metadataPrefix/from/until/set/offset/itemId}: the five fields the xoai library drives,
 * then the cursor. The cursor is the ordered key of the last record served, which lets the next page be
 * reached with a Solr range query instead of a deep offset skip; see {@link ResumptionCursor}.</p>
 *
 * <p>All six fields are required: a token of any other shape is refused as {@link BadResumptionToken} rather than
 * silently harvested through deep offset skips. The {@code itemId} field itself may be empty, which is what
 * the verbs that never reach the item repository -- ListSets -- hand out.</p>
 */
public class DSpaceResumptionTokenFormatter implements ResumptionTokenFormatter {
    private final static Logger log = LogManager.getLogger(DSpaceResumptionTokenFormatter.class);
    private final static String TOKEN_SEPARATOR = "/";

    private final ResumptionCursor cursor;

    public DSpaceResumptionTokenFormatter(ResumptionCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public ResumptionToken parse(String resumptionToken) throws BadResumptionToken {
        if (resumptionToken == null) {
            return new ResumptionToken();
        }
        String[] res = resumptionToken.split(TOKEN_SEPARATOR, -1);
        if (res.length != 6) {
            throw new BadResumptionToken();
        }
        try {
            String prefix = res[0].isEmpty() ? null : res[0];
            Date from = res[1].isEmpty() ? null : DateUtils.parse(res[1]);
            Date until = res[2].isEmpty() ? null : DateUtils.parse(res[2]);
            String set = res[3].isEmpty() ? null : res[3];
            int offset = Integer.parseInt(res[4]);
            if (!res[5].isEmpty()) {
                // Last part of the token (if exists) should be considered as the last item.id of the previous page.
                // if this information is decoded from the token, then move the cursor to this item.
                // The cursor will be used to build the query used to retrieve items to provide.
                // Force parsing the value as a UUID object ensures the token part value has a correct format.
                UUID itemUUID = UUID.fromString(res[5]);
                cursor.moveTo(itemUUID.toString());
            }
            return new ResumptionToken(offset, prefix, set, from, until);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BadResumptionToken();
        }
    }


    @Override
    public String format(ResumptionToken resumptionToken) {
        return new StringJoiner(TOKEN_SEPARATOR)
            .add(resumptionToken.hasMetadataPrefix() ? resumptionToken.getMetadataPrefix() : "")
            .add(resumptionToken.hasFrom() ? DateUtils.format(resumptionToken.getFrom()) : "")
            .add(resumptionToken.hasUntil() ? DateUtils.format(resumptionToken.getUntil()) : "")
            .add(resumptionToken.hasSet() ? resumptionToken.getSet() : "")
            .add(String.valueOf(resumptionToken.getOffset()))
            // The repository moves the cursor to the end of the page it just served, which is exactly where this
            // token resumes from.
            // It stays empty for the verbs that never reach the item repository, such as ListSets, whose tokens keep
            // working on their offset alone.
            .add(!cursor.isEmpty() ? cursor.valueOf() : "")
            .toString();
    }

}
