/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Object representing a doctoral dissertation object (text::thesis).
 * With some specific method concerning dissertation metadata.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class DissertationPublication extends Publication {

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String DOCUMENT_TYPE = "text::thesis";

    // CONSTRUCTOR =====================================================================================================
    protected DissertationPublication(Item item) throws InvalidModelEntityTypeException {
        super(item);
    }

    // FUNCTIONS =======================================================================================================
    @Override
    public boolean isWithdrawable() {
        return false;
    }
}
