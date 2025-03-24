/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CommentRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(ItemRest.CATEGORY + "." + ItemRest.PLURAL_NAME + "." + ItemRest.COMMENTS)
public class ItemCommentLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final CommentService commentService = UCLouvainServiceFactory.getInstance().getCommentService();

    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public Page<CommentRest> getComments(
            @Nullable HttpServletRequest request, UUID itemId,
            @Nullable Pageable optionalPageable, Projection projection
    ) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            List<Comment> itemComments = commentService.findByItem(context, item);
            Pageable pageable = optionalPageable != null ? optionalPageable : PageRequest.of(0, 20);
            return super.converter.toRestPage(itemComments, pageable, projection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
