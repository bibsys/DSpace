/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery;

import static org.dspace.uclouvain.discovery.UCLouvainDiscoverQueryBuilder.normalizeQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Query normalization performed before the user query reaches the Solr standard query parser.
 */
public class UCLouvainDiscoverQueryBuilderTest {

    @Test
    public void doiUrlBecomesQuotedBareDoi() {
        assertEquals("\"10.1080/15213269.2023.2242251\"",
                     normalizeQuery("https://doi.org/10.1080/15213269.2023.2242251"));
        assertEquals("\"10.1080/15213269.2023.2242251\"",
                     normalizeQuery("http://dx.doi.org/10.1080/15213269.2023.2242251"));
        assertEquals("\"10.1080/15213269.2023.2242251\"",
                     normalizeQuery("doi.org/10.1080/15213269.2023.2242251"));
        assertEquals("\"10.1080/15213269.2023.2242251\"",
                     normalizeQuery("  https://DOI.org/10.1080/15213269.2023.2242251  "));
    }

    @Test
    public void slashesAreEscaped() {
        assertEquals("10.1080\\/15213269.2023.2242251", normalizeQuery("10.1080/15213269.2023.2242251"));
        assertEquals("https:\\/\\/www.uclouvain.be\\/dial", normalizeQuery("https://www.uclouvain.be/dial"));
        assertEquals("dc.identifier.doi:10.1080\\/xyz", normalizeQuery("dc.identifier.doi:10.1080/xyz"));
    }

    @Test
    public void querySyntaxWithoutSlashIsUntouched() {
        assertEquals("search.resourceid:1234", normalizeQuery("search.resourceid:1234"));
        assertEquals("-search.resourceid:1234", normalizeQuery("-search.resourceid:1234"));
        assertEquals("\"climate change\" AND lang*", normalizeQuery("\"climate change\" AND lang*"));
        assertEquals("foo-bar", normalizeQuery("foo-bar"));
    }

    @Test
    public void blankQueryIsReturnedAsIs() {
        assertNull(normalizeQuery(null));
        assertEquals("", normalizeQuery(""));
        assertEquals("   ", normalizeQuery("   "));
    }
}
