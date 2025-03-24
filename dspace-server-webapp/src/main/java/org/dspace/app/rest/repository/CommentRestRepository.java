/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.app.rest.repository.patch.operation.PatchOperation.OPERATION_REPLACE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.model.CommentRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


@Component(CommentRest.CATEGORY + "." + CommentRest.PLURAL_NAME)
public class CommentRestRepository extends DSpaceRestRepository<CommentRest, UUID> {

    private final CommentService commentService = UCLouvainServiceFactory.getInstance().getCommentService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    // METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public Class<CommentRest> getDomainClass() {
        return CommentRest.class;
    }

    // REST METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // HTTP GET --------------------------------------------------------------------------------------------------------

    /**
     * Manage GET `api/core/comments/[UUID]` REST request.
     * Allow getting information's about a specific {@link org.dspace.uclouvain.content.Comment}.
     *
     * @param context the dspace application context
     * @param id the comment UUID to search
     * @return The REST representation of the comment
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'COMMENT', 'READ')")
    public CommentRest findOne(Context context, UUID id) {
        try {
            Comment comment = commentService.find(context, id);
            return (comment != null)
                ? converter.toRest(comment, utils.obtainProjection())
                : null;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * List all comments stored into the DSpace application.
     * This method is not implemented because without any filters, findAll doesn't have any sense.
     * @param context the dspace application context
     * @param pageable object embedding the requested pagination info
     * @return Nothing
     */
    @Override
    public Page<CommentRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(CommentRest.NAME, "findAll");
    }

    /**
     * Manage GET `api/core/comments/search/findAllByParent?id=[ItemUUID]` REST request.
     * Allows getting all comments linked to a specific parent item.
     *
     * @param id the parent item UUID
     * @param pageable object embedding the requested pagination info
     * @return the list of comments related to the specific item
     * @throws SQLException if any database exception occurred
     */
    @SearchRestMethod(name = "findAllByParent")
    @PreAuthorize("hasPermission(#id, 'COMMENT', 'READ')")
    public Page<AuthorizationRest> findAllByParent(
            @Parameter(value = "id", required = true) UUID id,
            Pageable pageable
    ) throws SQLException {
        Context context = obtainContext();
        Item item = itemService.find(context, id);
        if (item == null) {
            throw new DSpaceBadRequestException("Item " + id + " not found");
        }

        Sort sort = pageable.getSortOr(Sort.by(Sort.Direction.DESC, "created"));
        boolean asc = sort.isUnsorted() || (sort.isSorted() && sort.getOrderFor("created").isAscending());
        List<Comment> comments = commentService.findByItem(context, item, asc);
        return converter.toRestPage(comments, pageable, utils.obtainProjection());
    }

    /**
     * Manage POST `api/core/comments/?parent=[itemOwnerID]` REST request.
     * Allow creating a new comment on a specific item.
     *
     * @param context the dspace application context
     * @param parentID The uuid of the parent object retrieved from the query param.
     * @return the created comment
     * @throws AuthorizeException if any resource authorization errors occurred.
     * @throws SQLException if any database errors occurred.
     */
    @Override
    @PreAuthorize("hasPermission(#parentID, 'ITEM', 'EDIT')")
    protected CommentRest createAndReturn(Context context, UUID parentID) throws AuthorizeException, SQLException {
        CommentRest commentRest = null;
        try {
            commentRest = getPayload();
        } catch (Exception e) {
            throw new DSpaceBadRequestException(e.getMessage());
        }
        Item item = itemService.find(context, parentID);
        if (item == null) {
            throw new DSpaceBadRequestException("Item " + commentRest.getOwner() + " not found");
        }
        Comment comment = commentService.create(context, item, context.getCurrentUser(), commentRest.getContent());
        return converter.toRest(comment, utils.obtainProjection());
    }

    /**
     * Manage PATCH `api/core/comments/[UUID]` REST request.
     * Allows updating the content of an existing comment.
     *
     * @param context the dspace application context
     * @param request the http request
     * @param apiCategory the root api category (should be `core`)
     * @param model the api model (should be `comments`)
     * @param id the ID of the targeted comment object
     * @param patch the JSON Patch operation (https://tools.ietf.org/html/rfc6902)
     * @throws AuthorizeException if any resource authorization errors occurred.
     * @throws SQLException if any database errors occurred.
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'COMMENT', #patch)")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model,
            UUID id, Patch patch) throws AuthorizeException, SQLException {
        Comment comment = commentService.find(context, id);
        if (comment == null) {
            throw new DSpaceBadRequestException("Comment " + id + " not found");
        }

        // Check patch request payload contains only authorized paths.
        //   For 'Comment', we can only patch the `content` attribute. If any other `path` is defined, an exception
        //   must be raised.
        String patchedContent = getPatchOperation(patch, OPERATION_REPLACE, "content").findFirst().orElse(null);
        if (patchedContent == null) {
            throw new DSpaceBadRequestException("Patch operation must contains `content` path");
        }
        if (patch.getOperations().size() > 1) {
            throw new DSpaceBadRequestException("Patch path not supported. Only `content` must be patched");
        }

        commentService.updateCommentContent(context, comment, patchedContent);
    }

    /**
     * Manage DELETE `api/core/comment/[UUID]` REST request.
     * Allows deletion of a specific comment.
     *
     * @param context the dspace application context
     * @param uuid the comment ID to delete
     */
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID uuid) {
        try {
            Comment comment = commentService.find(context, uuid);
            if (comment == null) {
                throw new ResourceNotFoundException("Comment " + uuid + " not found");
            }
            commentService.delete(context, comment);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    // PRIVATE METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private CommentRest getPayload() throws UnprocessableEntityException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        try {
            ServletInputStream input = req.getInputStream();
            CommentRest payload = mapper.readValue(input, CommentRest.class);
            if (StringUtils.isBlank(payload.getContent())) {
                throw new UnprocessableEntityException("Cannot create a comment without any content");
            }
            return payload;
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }
    }

    private Stream<String> getPatchOperation(Patch patch, String opType, String opName) {
        Stream<Operation> operations = patch.getOperations().stream();
        if (opType != null) {
            operations = operations.filter(o -> o.getOp().equals(opType));
        }
        if (opName != null) {
            operations = operations.filter(o -> o.getPath().equals(opName));
        }
        return operations.map(o -> o.getValue().toString());
    }
}
