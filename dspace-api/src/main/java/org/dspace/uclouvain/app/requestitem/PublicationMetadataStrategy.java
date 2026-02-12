/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.app.requestitem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.dto.RegistrationDataChanges;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.springframework.lang.NonNull;

public class PublicationMetadataStrategy implements RequestItemAuthorExtractor {

    private static final Logger log = LogManager.getLogger(PublicationMetadataStrategy.class);

    private boolean publicationEmail;
    private boolean authorityEmail;

    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item) throws SQLException {
        try {
            Publication publication = PublicationFactory.build(item);
            List<RequestItemAuthor> recipients = new ArrayList<>();
            for (PublicationAuthor author : publication.getAuthors()) {
                String name = author.getName();
                if (publicationEmail) {
                    addIfValid(recipients, name, author.getEmail());
                }
                if (authorityEmail) {
                    addIfValid(recipients, name, author.getPrivateEmail());
                }
            }
            return recipients;
        } catch (InvalidModelEntityTypeException e) {
            log.warn("Unable to transform Item#{} as a Publication :: {}", item.getID(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // PRIVATE METHODS =================================================================================================

    /** Checks if an email is valid and adds a new recipient to the list if it matches criteria.
     *
     * @param list  the list of recipients to populate
     * @param name  the name of the author
     * @param email the email address to validate and add
     */
    private void addIfValid(List<RequestItemAuthor> list, String name, String email) {
        if (email != null) {
            boolean isPlaceHolder = email.trim().equals(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
            boolean isValidEmail = email.matches(RegistrationDataChanges.EMAIL_PATTERN);
            if (!isPlaceHolder && isValidEmail) {
                list.add(new RequestItemAuthor(name, email));
            }
        }
    }

    // GETTER & SETTER =================================================================================================
    //   No need getter for this bean
    public void setPublicationEmail(boolean value) {
        this.publicationEmail = value;
    }
    public void setAuthorityEmail(boolean value) {
        this.authorityEmail = value;
    }
}