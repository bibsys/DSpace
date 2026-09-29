/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Discovery search with user queries containing slashes (URLs, DOIs), which the Solr standard query parser
 * rejects unless escaped. See {@link org.dspace.uclouvain.discovery.UCLouvainDiscoverQueryBuilder}.
 */
public class DiscoverySearchQueryIT extends AbstractControllerIntegrationTest {

    private static final String DOI = "10.1080/15213269.2023.2242251";

    private Item publication;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Publications")
            .build();
        publication = ItemBuilder.createItem(context, publications)
            .withTitle("A publication with a DOI")
            .withDoiIdentifier(DOI)
            .build();
        ItemBuilder.createItem(context, publications)
            .withTitle("Another publication")
            .withDoiIdentifier("10.1000/other")
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void searchByDoiUrlFindsThePublication() throws Exception {
        expectSinglePublication(search("https://doi.org/" + DOI));
    }

    @Test
    public void searchByBareDoiFindsThePublication() throws Exception {
        expectSinglePublication(search(DOI));
    }

    @Test
    public void searchByArbitraryUrlDoesNotFail() throws Exception {
        search("https://www.uclouvain.be/dial")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(0)));
    }

    @Test
    public void fieldSyntaxStillWorks() throws Exception {
        expectSinglePublication(search("search.resourceid:" + publication.getID()));
    }

    @Test
    public void facetsEndpointAcceptsSlashes() throws Exception {
        getClient().perform(get("/api/discover/facets/dateIssued").param("query", "https://doi.org/" + DOI))
            .andExpect(status().isOk());
    }

    private ResultActions search(String query) throws Exception {
        return getClient().perform(get("/api/discover/search/objects").param("query", query));
    }

    private void expectSinglePublication(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.uuid",
                                is(publication.getID().toString())));
    }
}
