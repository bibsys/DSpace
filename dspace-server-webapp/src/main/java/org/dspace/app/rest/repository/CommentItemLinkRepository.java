/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.CommentRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.content.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Link repository for "item" subresource of an individual comment.
 * Allow retrieving the item parent resource of a {@link org.dspace.uclouvain.content.Comment}
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
@Component(CommentRest.CATEGORY + "." + CommentRest.PLURAL_NAME + "." + CommentRest.ITEM)
public class CommentItemLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    CommentService commentService;

    /**
     * Get the item related to a specific comment
     *
     * @param request The current request
     * @param commentId  The comment id
     * @param optionalPageable The pageable if applicable
     * @param projection The current Projection
     * @return The item rest representation of the given comment
     */
    public ItemRest getItem(@Nullable HttpServletRequest request,
                            UUID commentId,
                            @Nullable Pageable optionalPageable,
                            Projection projection) {
        try {
            Context context = obtainContext();
            Comment comment = commentService.find(context, commentId);
            if (comment == null) {
                throw new ResourceNotFoundException("No such comment: " + commentId);
            }
            Item item = comment.getOwner();
            if (item == null) {
                return null;
            }
            return converter.toRest(item, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
