/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.actions.factory;

import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.profileIngester.actions.CreateOrUpdateProfileAction;
import org.dspace.uclouvain.profileIngester.actions.DeleteProfileAction;
import org.dspace.uclouvain.profileIngester.actions.ProfileAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ProfileActionFactoryImpl extends ProfileActionFactory {

    @Autowired(required = true)
    CreateOrUpdateProfileAction createOrUpdateProfileAction;

    @Autowired(required = true)
    DeleteProfileAction deleteProfileAction;

    protected static final Logger logger = LoggerFactory.getLogger(ProfileActionFactoryImpl.class);

    @Override
    public ProfileAction getProfileActionClass(String action) {
        switch (action) {
            case PersonEventModel.ACTION_CREATE, PersonEventModel.ACTION_UPDATE:
                return createOrUpdateProfileAction;
            case PersonEventModel.ACTION_DELETE:
                return deleteProfileAction;
            default:
                logger.warn("An unknown action type \"" + action + "\" was found in the factory.");
                return null;
        }
    }
}
