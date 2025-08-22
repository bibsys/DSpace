/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.actions;

import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.external.esb.client.ESBClient;
import org.dspace.uclouvain.profileIngester.exceptions.ProfileActionException;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Generic profile action class.
 * Every action should extend this class.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public abstract class ProfileAction {
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected UCLouvainProfileService uclouvainProfileService;
    @Autowired
    protected ESBClient esbClient;
    @Autowired
    protected MetadataFieldService metadataFieldService;

    protected static final Logger logger = LoggerFactory.getLogger(ProfileAction.class);

    public abstract void process(Context context, PersonEventModel event) throws ProfileActionException;
}
