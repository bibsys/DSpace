/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import org.dspace.core.Context;
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
            String orcid, String fgs, String institution, String role, String authority, Integer place)
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
}
