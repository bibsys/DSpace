/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul.script;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;

/**
 * Command-line variant of {@link PulImport}: the running user is given by email with {@code -e}, as for the other
 * DSpace command-line scripts.
 */
public class PulImportCli extends PulImport {

    @Override
    protected void assignCurrentUser(Context context) throws Exception {
        String email = commandLine.getOptionValue('e');
        EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, email);
        if (ePerson == null) {
            throw new ParseException("No user with email " + email);
        }
        context.setCurrentUser(ePerson);
    }
}
