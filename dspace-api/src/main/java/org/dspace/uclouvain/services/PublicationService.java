/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

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
     * @param context     The current DSpace context.
     * @param name        The name of the author.
     * @param email       The email of the author.
     * @param orcid       The ORCID of the author.
     * @param fgs         The fgs identifier of the author.
     * @param institution The institution of the author.
     * @param role        The role of the author in the publication.
     * @param authority   The UUID of the profile item.
     * @param place       The place pf the author in the author list.
     * @return A PublicationAuthor object containing all the informations.
     * @throws PublicationSetAuthorException
     */
    public PublicationAuthor setAuthor(Context context, Publication publication, String name, String email,
            String orcid, String fgs, String institution, String role, UUID authority, Integer place)
            throws PublicationSetAuthorException;

    /**
     * Set author information in the publication (item) metadata.
     * 
     * @param context     The current DSpace context.
     * @param publication The publication to set an author for.
     * @param author      The author to persist.
     * @throws PublicationSetAuthorException if any error occurres while persisting
     *                                       information in publication.
     */
    public void setAuthor(Context context, Publication publication, PublicationAuthor author)
            throws PublicationSetAuthorException;

    /**
     * Find all publication linked to at least one of the given author.
     * @param context The current DSpace application context.
     * @param authors A list of authors to find publications for.
     * @return An stream of all the found publications.
     * @throws SearchServiceException If any solr exception occurres while searching.
     */
    public Stream<Publication> findByAuthors(Context context, List<Item> authors) throws SearchServiceException;

    /**
     * Find all publication linked to at least one of the given affiliation.
     * @param context The current DSpace application context.
     * @param affiliations A list of affiliation to find publications for.
     * @return An stream of all the found publications.
     * @throws SearchServiceException If any solr exception occurres while searching.
     */
    public Stream<Publication> findByAffiliations(Context context, List<OrgUnit> affiliations)
        throws SearchServiceException;

    /**
     * Find all publication linked to at specific funding.
     * @param context The current DSpace application context.
     * @param fundingOrganization The organization of the funding (required).
     * @param fundingProgram The program of the funding (optional).
     * @return A stream of all the publications linked to the given funding.
     * @throws SearchServiceException
     */
    public Stream<Publication> findByFunding(Context context, String fundingOrganization, String fundingProgram)
        throws SearchServiceException;

    /**
     * Find all publication items matching the given query and filter queries.
     * TODO: Improve this logic to handle more params (sort, filters...). It would be better to externalize this code.
     * @param context The current DSpace context.
     * @param query The main query to match.
     * @param filterQueries Additional filter queries to match.
     * @return A stream of all found publications based on the given query.
     * @throws SearchServiceException
     */
    public Stream<Publication> findPublications(Context context, String query, Map<String, String> filterQueries)
        throws SearchServiceException;
}
