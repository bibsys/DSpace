/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.packager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.validation.model.ValidationError;

/**
 * Detects an ingested item whose DOI is already the DOI of an archived item.
 *
 * <p>Two DOI are the same when their normalized forms are equal: the case and the resolver prefix
 * ({@code https://doi.org/}, {@code doi:}, ...) are ignored. So {@code https://doi.org/10.1234/ABC} and
 * {@code 10.1234/abc} are the same DOI.</p>
 *
 * <p>Only the archived items count: a submission in progress or a withdrawn item is not a duplicate.</p>
 */
public class DoiDuplicateChecker {

    private static final String DOI_FIELD = Publication.DOI_IDENTIFIER_FIELD;

    /** The prefixes which can precede a bare DOI: "https://doi.org/", "http://dx.doi.org/", "doi:", ... */
    private static final Pattern DOI_PREFIX =
        Pattern.compile("^((https?://)?(dx\\.)?doi\\.org/|doi:(//)?)", Pattern.CASE_INSENSITIVE);

    /** More than enough: the search only returns the items whose DOI looks like the searched one. */
    private static final int MAX_CANDIDATES = 200;

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final SearchService searchService = SearchUtils.getSearchService();

    /**
     * Checks every DOI of an item.
     *
     * @param context the DSpace context
     * @param item    the ingested item, not yet archived
     * @return one error per DOI already used by an archived item, with the handle of that item; empty if there
     *         is none
     * @throws SearchServiceException if the search of the archived items fails
     */
    public List<ValidationError> check(Context context, Item item) throws SearchServiceException {
        List<ValidationError> errors = new ArrayList<>();
        for (MetadataValue doi : itemService.getMetadataByMetadataString(item, DOI_FIELD)) {
            Optional<Item> duplicate = findArchivedItemWithDoi(context, normalize(doi.getValue()));
            if (duplicate.isPresent()) {
                errors.add(new DoiDuplicateError(DOI_FIELD + "/" + doi.getPlace(), duplicate.get().getHandle()));
            }
        }
        return errors;
    }

    /**
     * Returns the form of a DOI used to compare it: without resolver prefix and in lower case.
     *
     * @param doi a DOI as found in the metadata, for example "https://doi.org/10.1234/ABC"
     * @return the normalized DOI, for example "10.1234/abc"
     */
    static String normalize(String doi) {
        String bareDoi = DOI_PREFIX.matcher(StringUtils.trimToEmpty(doi)).replaceFirst("");
        return bareDoi.toLowerCase(Locale.ROOT);
    }

    /**
     * The search index gives the archived items whose DOI looks like the searched one (it ignores the case and
     * finds a DOI stored with a prefix). The DOI of these candidates are then compared exactly, because the
     * index also matches a longer DOI starting with the searched one.
     */
    private Optional<Item> findArchivedItemWithDoi(Context context, String normalizedDoi)
            throws SearchServiceException {
        if (normalizedDoi.isEmpty()) {
            return Optional.empty();
        }
        DiscoverQuery query = new DiscoverQuery();
        query.setQuery("doi:\"" + searchService.escapeQueryChars(normalizedDoi) + "\"");
        query.addFilterQueries("search.resourcetype:" + IndexableItem.TYPE);
        query.addFilterQueries("archived:true");
        query.setMaxResults(MAX_CANDIDATES);

        for (IndexableObject<?, ?> candidate : searchService.search(context, query).getIndexableObjects()) {
            Item archivedItem = (Item) candidate.getIndexedObject();
            if (hasDoi(archivedItem, normalizedDoi)) {
                return Optional.of(archivedItem);
            }
        }
        return Optional.empty();
    }

    private boolean hasDoi(Item item, String normalizedDoi) {
        return itemService.getMetadataByMetadataString(item, DOI_FIELD).stream()
            .anyMatch(doi -> normalize(doi.getValue()).equals(normalizedDoi));
    }
}
