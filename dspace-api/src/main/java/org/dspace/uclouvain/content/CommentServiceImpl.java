/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.event.Event;
import org.dspace.uclouvain.content.dao.CommentDAO;
import org.dspace.uclouvain.content.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the {@link org.dspace.uclouvain.content.service.CommentService}
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public class CommentServiceImpl implements CommentService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Comment.class);

    @Autowired
    CommentDAO commentDAO;

    // CRUD METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Create methods --------------------------------------------------------------------------------------------------
    @Override
    public Comment create(Context context, Item owner, EPerson author, String content) throws SQLException {
        Comment comment = buildCommentSkeleton(owner, content);
        comment.setAuthorAuthority(author);
        comment.setAuthorName(author.getFullName());
        return createComment(context, comment);
    }

    @Override
    public Comment create(Context context, Item owner, String authorName, String content) throws SQLException {
        Comment comment = buildCommentSkeleton(owner, content);
        comment.setAuthorName(authorName);
        return createComment(context, comment);
    }

    @Override
    public Comment createSystemComment(Context context, Item owner, String content) throws SQLException {
        return create(context, owner, Comment.SYSTEM_COMMENT_OWNER, content);
    }

    // Update methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public void updateCommentContent(Context context, Comment comment, String content) throws SQLException {
        comment.setContent(content);
        comment.setModified();

        context.addEvent(new Event(Event.MODIFY, Constants.COMMENT, comment.getID(), "content, modifiedDate"));
        if (log.isDebugEnabled()) {
            log.debug(LogHelper.getHeader(context, "update_comment", "comment_id=" + comment.getID()));
        }

        commentDAO.save(context, comment);
    }

    // Delete methods --------------------------------------------------------------------------------------------------
    @Override
    public void delete(Context context, Comment comment) throws SQLException {
        log.info(LogHelper.getHeader(context, "delete_comment", "comment_id=" + comment.getID()));
        commentDAO.delete(context, comment);

        context.addEvent(new Event(Event.DELETE, Constants.COMMENT, comment.getID(), null));
    }
    @Override
    public void deleteAllItemComments(Context context, Item item) throws SQLException {
        for (Comment c : findByItem(context, item)) {
            delete(context, c);
        }
    }

    // Search methods --------------------------------------------------------------------------------------------------
    @Override
    public Comment find(Context context, UUID id) throws SQLException {
        Comment comment = commentDAO.findByID(context, Comment.class, id);
        if (log.isDebugEnabled()) {
            log.debug((comment == null)
                ? LogHelper.getHeader(context, "find_comment", "not_found,comment_id=" + id)
                : LogHelper.getHeader(context, "find_comment", "comment_id=" + id)
            );
        }
        return comment;
    }

    @Override
    public List<Comment> findAll(Context context) throws SQLException {
        return commentDAO.findAll(context, Comment.class);
    }

    @Override
    public List<Comment> findByItem(Context context, Item item) throws SQLException {
        return commentDAO.findByItem(context, item);
    }

    @Override
    public int countCommentByItem(Context context, Item item) throws SQLException {
        return commentDAO.countCommentsForItem(context, item);
    }

    // PRIVATE METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**x
     * Build the skeleton for a new {@link org.dspace.uclouvain.content.Comment} with basic information's
     *
     * @param owner the {@link org.dspace.content.Item} owner of the comment.
     * @param content the content of the comment.
     * @return the comment model
     * @throws SQLException for any errors during comment creation
     */
    private Comment buildCommentSkeleton(Item owner, String content) throws SQLException {
        if (StringUtils.isBlank(content)) {
            throw new SQLException("Comment must be created with non-blank content");
        }
        Comment comment = new Comment();
        comment.setOwner(owner);
        comment.setContent(content);
        comment.setCreationDate(Date.from(Instant.now()));
        return comment;
    }

    /**
     * Create and save a {@link org.dspace.uclouvain.content.Comment} into the database by calling the DOA.
     *
     * @param context the application context.
     * @param comment the {@link org.dspace.uclouvain.content.Comment} to insert into the database.
     * @return the created comment.
     * @throws SQLException if any database errors occurred.
     */
    private Comment createComment(Context context, Comment comment) throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug(LogHelper.getHeader(context, "create_comment", "comment_id=" + comment.getID()));
        }
        context.addEvent(new Event(Event.CREATE, Constants.COMMENT, comment.getID(), null));
        return commentDAO.create(context, comment);
    }
}
