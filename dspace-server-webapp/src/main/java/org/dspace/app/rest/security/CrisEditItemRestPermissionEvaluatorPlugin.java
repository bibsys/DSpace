/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.core.Constants.ITEM;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


/**
 * Plugin used to evaluate if someone could edit an {@link org.dspace.content.Item} by checking CRIS `editModes` rules.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
@Component
public class CrisEditItemRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    @Autowired
    private EditItemModeService modeService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RequestService requestService;

    @Override
    public boolean hasDSpacePermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            DSpaceRestPermission permission
    ) {
        permission = DSpaceRestPermission.convert(permission);
        if (Constants.getTypeID(targetType) != ITEM || permission != DSpaceRestPermission.EDIT) {
            return false;
        }

        Item item = null;
        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        try {
            item = itemService.find(context, UUID.fromString(targetId.toString()));
        } catch (SQLException sqle) {
            throw new SQLRuntimeException(sqle);
        }
        if (Objects.isNull(item)) {
            // this is necessary to allow 404 instead than 403
            return true;
        }
        return modeService.canEdit(context, item);
    }
}
