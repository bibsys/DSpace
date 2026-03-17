/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Set of security evaluator on items.
 */
@Component(value = "itemSecurity")
public class ItemSecurityEvaluator {

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ResearcherProfileService profileService;

    /**
     * Check if the current logged user is an author of the provided item.
     * 
     * @param itemId The id of the item to check.
     * @return True if the currently logged user is an author of the item, else false.
     */
    public boolean isAuthor(UUID itemId) {
        Context context = ContextUtil.obtainContext(request);
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        try {
            ResearcherProfile profile = profileService.findById(context, currentUser.getID());
            if (profile == null) {
                return false;
            }
            Item item = itemService.find(context, itemId);
            if (item == null) {
                return false;
            }
            Publication publication = PublicationFactory.build(item);
            return publication.getAuthors()
                .stream()
                .map(PublicationAuthor::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(author -> Objects.equals(author.getItemId(), profile.getItemId()));
        } catch (Exception e) {
            return false;
        }
    }
}
