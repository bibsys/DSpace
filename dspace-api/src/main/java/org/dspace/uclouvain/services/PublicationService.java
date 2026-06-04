/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.core.model.exceptions.PublicationSetAuthorException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.export.services.UCLouvainExportService;

public interface PublicationService {

    /**
     * Add a new author to a publication (item) metadata at a specific place.
     * 
     * @param context     The current DSpace context
     * @param name        The name of the author
     * @param email       The email of the author
     * @param orcid       The ORCID of the author
     * @param fgs         The fgs identifier of the author
     * @param institution The institution of the author
     * @param role        The role of the author in the publication
     * @param authority   The UUID of the profile item
     * @param place       The place pf the author in the author list
     * @param override    If set to true, override the existing metadata from the publication.
     * @return the corresponding publication author
     * @throws PublicationSetAuthorException if any error occurred while persisting information in publication.
     */
    default PublicationAuthor setAuthor(Context context, Publication publication, String name, String email,
            String orcid, String fgs, String institution, String role, UUID authority, Integer place, boolean override)
            throws PublicationSetAuthorException {
        PublicationAuthor author = new PublicationAuthor()
            .setName(name)
            .setEmail(email)
            .setOrcidID(orcid)
            .setFgs(fgs)
            .setRole(role)
            .setInstitution(institution)
            .setAuthority(authority)
            .setPlace(place);
        this.setAuthor(context, publication, author, override);
        return author;
    }

    /**
     * Set author information in the publication (item) metadata.
     * 
     * @param context     The current DSpace context
     * @param publication The publication to set an author for
     * @param author      The author to persist
     * @param override    If set to true, override the existing metadata from the publication.
     * @return the corresponding publication author
     * @throws PublicationSetAuthorException if any error occurred while persisting information in publication.
     */
    PublicationAuthor setAuthor(Context context, Publication publication, PublicationAuthor author, boolean override)
        throws PublicationSetAuthorException;

    /**
     * Find all publication linked to at least one of the given author.
     *
     * @param context The current DSpace application context
     * @param identifiers The list of author identifier to search for.
     *                    Each author identifier is a pair of identifier type (uuid, fgs, name, ...) and
     *                    identifier value.
     * @param filters A map of filters to use to filter solr query response
     * @param sort the option to use to sort publications
     * @param direction the sort direction to apply on sort option (ASC or DESC)
     * @return stream containing all found publications
     * @throws SearchServiceException If any solr exception occurred while searching.
     */
    Stream<Publication> findByAuthors(
        Context context,
        List<Pair<String, String>> identifiers,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException;

    /**
     * Find all publication linked to at least one of the given affiliation names.
     *
     * @param context The current DSpace application context
     * @param names A list of affiliation names to find publications for
     * @param filters A map of filters to use to filter solr query response
     * @param sort the option to use to sort publications
     * @param direction the sort direction to apply on sort option (ASC or DESC)
     * @return A stream of all the found publications
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findByAffiliationNames(
        Context context,
        List<String> names,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException;

    /**
     * Find all publication linked to at least one of the given affiliation uuids.
     *
     * @param context The current DSpace application context
     * @param uuids A list of affiliation uuids to find publications for
     * @param includeDescendant is the publication related to descendant entities must be included
     * @param filters A map of filters to use to filter solr query response
     * @param sort the option to use to sort publications
     * @param direction the sort direction to apply on sort option (ASC or DESC)
     * @return A stream of all the found publications
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findByAffiliationUUIDs(
        Context context,
        List<String> uuids,
        boolean includeDescendant,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException;

    /**
     * Find all publication linked to at specific funding.
     *
     * @param context The current DSpace application context
     * @param fundingOrganization The organization of the funding (required)
     * @param fundingProgram The program of the funding (optional)
     * @param filters A map of filters to use to filter solr query response
     * @param sort the option to use to sort publications
     * @param direction the sort direction to apply on sort option (ASC or DESC)
     * @return A stream of all the publications linked to the given funding
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findByFunding(
        Context context,
        String fundingOrganization,
        String fundingProgram,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException, ParseException;

    /**
     * Find all publication items matching the given query and filter queries.
     *
     * @param context The current DSpace context
     * @param query The main query to match
     * @param filterQueries Additional filter queries to match
     * @param sort the option to use to sort publications
     * @param sortDirection the sort direction to apply on sort option (ASC or DESC)
     * @return A stream of all found publications based on the given query
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findPublications(
        Context context,
        String query,
        Map<String, String> filterQueries,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER sortDirection
    ) throws SearchServiceException;

    /**
     * Determine if the current logged user is an author of the publication
     *
     * @param context The current DSpace context
     * @param item the item to analyze (should ba a publication)
     * @return True if any publication author is linked to the current logged user
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization exception occurred
     */
    boolean isAuthorOfPublication(Context context, Item item) throws SQLException, AuthorizeException;
}
