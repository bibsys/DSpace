package org.dspace.uclouvain.authority;

import org.dspace.content.authority.Choices;
import org.dspace.content.authority.ItemAuthority;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.content.Item;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.web.ContextUtil;

/**
 * Simple abstract authority class that can be extended to create simple authorities.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public abstract class PublicationAuthority implements ChoiceAuthority {
    private static Logger logger = LogManager.getLogger(ItemAuthority.class);
    protected DSpace dspace = new DSpace();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected SearchService searchService = dspace.getServiceManager().getServiceByName(
        "org.dspace.discovery.SearchService", SearchService.class);

    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale, true);
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        return getMatches(text, start, limit, locale, false);
    }

    /**
     * Get the matches for the given text (query).
     * @param text: the query.
     * @param start: the start index.
     * @param limit: the limit of results.
     * @param locale: the locale.
     * @param onlyExactMatches: if true, only exact matches will be returned.
     * @return: A Choices object that contains the results of the search.
     */
    protected Choices getMatches(String text, int start, int limit, String locale, boolean onlyExactMatches) {
        if (limit <= 0) {
            limit = 20;
        }

        SolrClient solr = searchService.getSolrSearchCore().getSolr();
        if (Objects.isNull(solr)) {
            logger.error("unable to find solr instance");
            return new Choices(Choices.CF_UNSET);
        }

        String query = text;
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setStart(start);
        solrQuery.setRows(limit);
        solrQuery.addFilterQuery("search.resourcetype:" + Item.class.getSimpleName());
        solrQuery.addFilterQuery(getEntityTypeFilterString());

        try {
            Context context = getContext();
            QueryResponse queryResponse = solr.query(solrQuery);
            List<Choice> choices = new ArrayList<Choice>();

            SolrDocumentList results = queryResponse.getResults();
            for (int i = 0; i < results.size(); i++) {
                try {
                    SolrDocument doc = results.get(i);
                    String uuid = (String) doc.getFieldValue("search.resourceid");
                    String title = ((ArrayList<String>) doc.getFieldValue("dc.title"))
                            .stream()
                            .findFirst()
                            .orElse(text);
                    Item item = itemService.find(context, UUID.fromString(uuid));
                    if (item != null) {
                        choices.add(new Choice(uuid, title, title, this.generateExtras(item)));
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            Choice[] final_results = new Choice[choices.size()];
            final_results = choices.toArray(final_results);
            long numFound = queryResponse.getResults().getNumFound();

            return new Choices(final_results, 0, (int) numFound, Choices.CF_AMBIGUOUS, false, -1);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Choices(Choices.CF_UNSET);
        }
    }

    protected Context getContext() {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return context != null ? context : new Context();
    }
    
    public abstract String getPluginInstanceName();
    public abstract void setPluginInstanceName(String pluginInstanceName);
    public abstract String getLabel(String key, String locale);

    protected abstract Map<String, String> generateExtras(Item item) throws Exception;
    protected abstract String getEntityTypeFilterString();
}
