/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;

public abstract class DissertationNotifyEmail extends AbstractNotifyEmail {
    protected List<String> forcedManagers;

    public DissertationNotifyEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
        forcedManagers = Arrays.asList(getConfigurationAttributes("additional-cc-addresses"));
    }

    @Override
    protected List<String> getCCAddresses() {
        List<String> ccAddresses = new ArrayList<>();
        ccAddresses.addAll(forcedManagers);
        return ccAddresses.stream().distinct().toList();
    }
}
