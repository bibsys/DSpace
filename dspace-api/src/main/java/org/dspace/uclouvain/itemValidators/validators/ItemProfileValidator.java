/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemValidators.validators;

import java.util.Map;
import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.uclouvain.itemValidators.ItemValidator;
import org.dspace.uclouvain.itemValidators.exceptions.ItemValidationException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validate a Profile Item.
 * Check if the identifiers of the item are already existing in another profile item.
 * Email, fgs and ORCID have to be unique and cannot be present in two different profiles.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ItemProfileValidator implements ItemValidator {

    @Autowired
    ResearcherProfileService researcherProfileService;

    public void validate(Context context, Item item) throws ItemValidationException {
        ResearcherProfile profile = new ResearcherProfile(item, false);
        Map<String, String> identifiers = researcherProfileService.getProfileIdentifiers(profile);
        if (identifiers.isEmpty()) {
            return;
        }
        boolean hasOtherProfile = researcherProfileService.findByIdentifiers(context, identifiers)
            .stream()
            .anyMatch(matchingProfile -> !Objects.equals(matchingProfile.getItem().getID(), item.getID()));
        if (hasOtherProfile) {
            // Found an already existing profile with the same identifiers.
            throw new ItemValidationException(
                "Found an already existing profile with the same identifiers.",
                "item.validator.person.not-unique.message"
            );
        }
    }
}
