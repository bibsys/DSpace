/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.behavior;

import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/** Interface to describe a consumer activation rule */
public interface ConsumerActivationRule {
    boolean isValid(Context context, DSpaceObject dso) throws SQLException;
}
