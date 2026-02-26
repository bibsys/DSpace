/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.core.model.exceptions.PublicationSetAuthorException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;

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
     * @return the corresponding publication author
     * @throws PublicationSetAuthorException if any error occurred while persisting information in publication.
     */
    default PublicationAuthor setAuthor(Context context, Publication publication, String name, String email,
            String orcid, String fgs, String institution, String role, UUID authority, Integer place)
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
        this.setAuthor(context, publication, author);
        return author;
    }

    /**
     * Set author information in the publication (item) metadata.
     * 
     * @param context     The current DSpace context
     * @param publication The publication to set an author for
     * @param author      The author to persist
     * @return the corresponding publication author
     * @throws PublicationSetAuthorException if any error occurred while persisting information in publication.
     */
    PublicationAuthor setAuthor(Context context, Publication publication, PublicationAuthor author)
            throws PublicationSetAuthorException;

    /**
     * Find all publication linked to at least one of the given author.
     *
     * @param context The current DSpace application context
     * @param identifiers The list of author identifier to search for.
     *                    Each author identifier is a pair of identifier type (uuid, fgs, name, ...) and
     *                    identifier value.
     * @return stream containing all found publications
     * @throws SearchServiceException If any solr exception occurred while searching.
     */
    Stream<Publication> findByAuthors(Context context, List<Pair<String, String>> identifiers)
            throws SearchServiceException;

    /**
     * Find all publication linked to at least one of the given affiliation.
     *
     * @param context The current DSpace application context
     * @param affiliations A list of affiliation to find publications for
     * @return A stream of all the found publications
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findByAffiliations(Context context, List<OrgUnit> affiliations)
        throws SearchServiceException;

    /**
     * Find all publication linked to at specific funding.
     *
     * @param context The current DSpace application context
     * @param fundingOrganization The organization of the funding (required)
     * @param fundingProgram The program of the funding (optional)
     * @return A stream of all the publications linked to the given funding
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findByFunding(Context context, String fundingOrganization, String fundingProgram)
        throws SearchServiceException;

    /**
     * Find all publication items matching the given query and filter queries.
     * TODO: Improve this logic to handle more params (sort, filters...). It would be better to externalize this code.
     *
     * @param context The current DSpace context
     * @param query The main query to match
     * @param filterQueries Additional filter queries to match
     * @return A stream of all found publications based on the given query
     * @throws SearchServiceException If any solr exception occurred while searching
     */
    Stream<Publication> findPublications(Context context, String query, Map<String, String> filterQueries)
        throws SearchServiceException;

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

    /**
     * Determine if the item could be retired depending on the context (if the item is a publication)
     *
     * @param context the current DSpace context
     * @param item the item to analyze
     * @return true if the item can be withdrawn; false otherwise (or any exception occurred)
     */
    boolean authorizeWithdrawItem(Context context, Item item);
}
