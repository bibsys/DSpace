/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractUnitTest;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.XSLTIngestionCrosswalk;
import org.dspace.core.factory.CoreServiceFactory;
import org.junit.Test;

/**
 * The ONIX stylesheet is only reachable by the import if `crosswalk.submission.ONIX.stylesheet` (pul.cfg) is loaded
 * and points to an existing file: this resolves the named plugin the way the import script will.
 */
public class OnixCrosswalkRegistrationTest extends AbstractUnitTest {

    @Test
    public void onixIngestionCrosswalkIsRegistered() {
        IngestionCrosswalk crosswalk = (IngestionCrosswalk) CoreServiceFactory.getInstance().getPluginService()
            .getNamedPlugin(IngestionCrosswalk.class, "ONIX");
        assertNotNull("no ingestion crosswalk named ONIX: is pul.cfg included and the stylesheet present?", crosswalk);
        assertTrue(crosswalk instanceof XSLTIngestionCrosswalk);
    }
}
