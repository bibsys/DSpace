package org.dspace.uclouvain.core.utils;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utility class for authorization checks.
 * 
 * @Author: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class AuthorizationUtils {

    @Autowired
    private ItemUtils itemUtils;

    @Autowired
    private ItemService itemService;

    /**
     * Checks if the current user is a manager of the item's collection.
     * Retrieve the group for the collection that corresponds to the reviewer role and check if the current user is a member.
     * @param item: The item to check permission for.
     * @param currentUser: The current logged user.
     * @param context: The current DSpace context.
     * @return True if the user is a manager of the item's collection else false.
     * @throws SQLException
     */
    public boolean isManagerOfItem(Context context, Item item, EPerson currentUser) throws SQLException {
        return this.itemUtils.getManagersOfItem(context, item).contains(currentUser);
    }

    /**
     * Checks if the given user is a promoter of the item.
     * @param item: The item to check permission for.
     * @param person: The user to test for promoter membership.
     * @return True if the user is a promoter of the item, false otherwise.
     */
    public boolean isPromoterOfItem(Item item, EPerson person) {
        // TODO: Use config for field name
        List<String> promoters = this.itemService.getMetadata(
            item, "advisors", "email", null, null
        ).stream().map(metadataValue -> metadataValue.getValue()).collect(Collectors.toList());
    
        return promoters.contains(person.getEmail());
    }
}
