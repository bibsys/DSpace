/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.content.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Plugin used to evaluate permission about `comment` resource access.
 *   * Comment could be read if the current user has READ permission on parent item.
 *   * Comment could be written/patched if the current user can EDIT the parent item.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
@Component
public class CommentRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger(CommentRestPermissionEvaluatorPlugin.class);

    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private EditItemModeService modeService;
    @Autowired
    private RequestService requestService;


    @Override
    public boolean hasDSpacePermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            DSpaceRestPermission permission
    ) {
        if (Constants.getTypeID(targetType) != Constants.COMMENT) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());

        Item item = getRelatedItem(context, UUID.fromString(targetId.toString()));
        if (Objects.isNull(item)) {
            // this is necessary to allow 404 instead than 403
            return true;
        }
        try {
            switch (DSpaceRestPermission.convert(permission)) {
                case READ:
                    return authorizeService.authorizeActionBoolean(context, item, Constants.READ);
                case WRITE:
                    return modeService.canEdit(context, item);
                default:
                    return false;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }


    /** Get the item related a comment
     *
     * @param context the dspace application context
     * @param commentId the comment ID to load
     * @return the comment parent item.
     */
    private Item getRelatedItem(Context context, UUID commentId) {
        if (commentId == null) {
            return null;
        }
        try {
            Comment comment = commentService.find(context, commentId);
            return (comment != null) ? comment.getOwner() : null;
        } catch (SQLException sqle) {
            throw new SQLRuntimeException(sqle);
        }
    }

}
