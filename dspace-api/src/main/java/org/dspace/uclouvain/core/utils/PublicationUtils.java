/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;

/**
 * Set of utils method for publication objects.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class PublicationUtils {

    protected PublicationUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean existsPersistentRecipient(Item item) {
        try {
            Publication publication = PublicationFactory.build(item);
            return !publication.getAuthorsEmails(true, true).isEmpty();
        } catch (InvalidModelEntityTypeException imete) {
            return false;
        }
    }
}
