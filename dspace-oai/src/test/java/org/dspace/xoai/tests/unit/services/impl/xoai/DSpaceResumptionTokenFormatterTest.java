/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl.xoai;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import com.lyncode.xoai.dataprovider.core.ResumptionToken;
import com.lyncode.xoai.dataprovider.exceptions.BadResumptionToken;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.services.impl.xoai.DSpaceResumptionTokenFormatter;
import org.junit.Test;

public class DSpaceResumptionTokenFormatterTest {

    private static final String SET = "col_123456789_2";

    private final String itemId = UUID.randomUUID().toString();
    private final ResumptionCursor cursor = new ResumptionCursor();
    private final DSpaceResumptionTokenFormatter underTest = new DSpaceResumptionTokenFormatter(cursor);

    @Test
    public void theCursorReachedWhileServingAPageIsAppendedToTheToken() {
        cursor.moveTo(itemId);

        assertThat(underTest.format(new ResumptionToken(200, "oai_dc", SET, null, null)),
                   is("oai_dc///" + SET + "/200/" + itemId));
    }

    @Test
    public void aVerbThatNeverReachedTheItemRepositoryKeepsAnOffsetOnlyToken() {
        // ListSets paginates through its own repository, which leaves the cursor untouched.
        assertThat(underTest.format(new ResumptionToken(200, null, null, null, null)),
                   is("////200/"));
    }

    @Test
    public void aTokenCarryingACursorIsParsedIntoThePositionItNames() throws Exception {
        ResumptionToken token = underTest.parse("oai_dc///" + SET + "/200/" + itemId);

        assertThat(token.getOffset(), is(200));
        assertThat(token.getMetadataPrefix(), is("oai_dc"));
        assertThat(token.getSet(), is(SET));
        assertThat(cursor.valueOf(), is(itemId));
    }

    @Test
    public void aTokenWithoutACursorFieldIsRefused() {
        // Harvesting it would mean skipping 200 documents in Solr to serve the page.
        assertRefused("oai_dc///" + SET + "/200");
    }

    @Test
    public void aTokenWithMoreFieldsThanExpectedIsRefused() {
        assertRefused("oai_dc///" + SET + "/200/" + itemId + "/" + itemId);
    }

    @Test
    public void anEmptyCursorIsAcceptedForTheVerbsThatHaveNoRecords() throws Exception {
        // ListSets paginates through its own repository and hands out no item.id.
        ResumptionToken token = underTest.parse("////200/");

        assertThat(token.getOffset(), is(200));
        assertTrue(cursor.isEmpty());
    }

    @Test
    public void aCursorThatIsNotAnItemIdIsRejectedInsteadOfReachingSolr() {
        // The token is client supplied and the cursor is interpolated into a Solr query.
        assertRefused("oai_dc///" + SET + "/200/* TO *] OR item.public:false");
    }

    @Test
    public void whatIsFormattedCanBeParsedBack() throws Exception {
        cursor.moveTo(itemId);
        String formatted = underTest.format(new ResumptionToken(300, "mets", SET, null, null));

        ResumptionCursor parsedCursor = new ResumptionCursor();
        ResumptionToken parsed = new DSpaceResumptionTokenFormatter(parsedCursor).parse(formatted);

        assertThat(parsed.getOffset(), is(300));
        assertThat(parsed.getSet(), is(SET));
        assertThat(parsedCursor.valueOf(), is(itemId));
    }

    private void assertRefused(String token) {
        try {
            underTest.parse(token);
            fail("expected a bad resumption token for " + token);
        } catch (BadResumptionToken expected) {
            assertTrue("nothing must be handed to the repository", cursor.isEmpty());
        }
    }
}
