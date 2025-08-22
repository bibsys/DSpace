/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.actions;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.profileIngester.exceptions.ProfileActionException;

/**
 * Action to delete a profile if it exists and has no publication linked to it.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DeleteProfileAction extends ProfileAction {
    /**
     * Extract the fgs from the provided event.
     * If a profile exists for this identifier and it is not linked to any publication item, then delete it.
     * 
     * @param context The current DSpace context.
     * @param event The event to extract the identifier of the object to delete.
     * @throws ProfileActionException If an error occurred and the profile could not be delete.
     */
    public void process(Context context, PersonEventModel event) throws ProfileActionException {
        String fgs = event.getFgs();
        try {
            Item profile = uclouvainProfileService.findById(context, fgs);
            if (profile == null) {
                logger.info(
                    "[DELETE CANCELED] Profile with fgs " + fgs + " doesn't exists."
                );
                return;
            }
            if (!uclouvainProfileService.findLinkedPublications(context, profile).isEmpty()) {
                logger.info(
                    "[DELETE CANCELED] Profile with fgs " + fgs + " has linked publications."
                );
                return;
            }
            itemService.delete(context, profile);
            // Commit to apply changes
            context.commit();
            logger.info("[DELETE PROFILE] Deleted existing profile for fgs " + fgs);
        } catch (Exception e) {
            throw new ProfileActionException("Could not delete the desired profile: " + e.getLocalizedMessage(), e);
        }
    }
}
