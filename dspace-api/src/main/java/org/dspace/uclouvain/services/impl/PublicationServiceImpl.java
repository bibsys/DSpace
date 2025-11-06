/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.exceptions.PublicationSetAuthorException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.services.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;

public class PublicationServiceImpl implements PublicationService {

    @Autowired
    ItemService itemService;

    public PublicationAuthor setAuthor(Context context, Publication publication,
            String name, String email, String orcid, String fgs,
            String institution, String role, String authority, Integer place)
            throws PublicationSetAuthorException {
        PublicationAuthor author = new PublicationAuthor()
                .setName(name)
                .setEmail(email)
                .setOrcidID(orcid)
                .setRole(role)
                .setInstitution(institution)
                .setAuthority(UUID.fromString(authority))
                .setPlace(place);
        this.setAuthor(context, publication, author);
        return author;
    }

    public void setAuthor(Context context, Publication publication, PublicationAuthor author)
            throws PublicationSetAuthorException {
        Item item = publication.getItem();
        try {
            String authority = (author.getAuthority() != null) ? author.getAuthority().getID().toString() : null;
            int confidence = isNotEmpty(authority) ? CF_ACCEPTED : CF_UNSET;
            int place = author.getPlace();

            // Name, email, fgs and role are mandatory so no need to check for existing value.
            itemService.setMetadataInPlace(
                    context, item, Publication.AUTHOR_NAME_FIELD, null, author.getName(), authority,
                    place,
                    confidence);
            itemService.setMetadataInPlace(
                    context, item, Publication.AUTHOR_EMAIL_FIELD, null, author.getEmail(),
                    authority, place,
                    confidence);
            itemService.setMetadataInPlace(
                    context, item, Publication.AUTHOR_FGS_FIELD, null, author.getFgs(), authority,
                    place, confidence);
            itemService.setMetadataInPlace(
                    context, item, Publication.AUTHOR_ROLE_FIELD, null, author.getRole(), null,
                    place, CF_UNSET);

            if (author.getOrcidID() != null) {
                itemService.setMetadataInPlace(
                        context, item, Publication.AUTHOR_ORCID_FIELD, null,
                        author.getOrcidID(), authority, place,
                        confidence);
            }
            if (author.getInstitution() != null) {
                itemService.setMetadataInPlace(
                        context, item, Publication.AUTHOR_INSTITUTION_FIELD, null,
                        author.getInstitution(), null, place,
                        confidence);
            }
        } catch (Exception e) {
            throw new PublicationSetAuthorException(item, author);
        }
    }

}
