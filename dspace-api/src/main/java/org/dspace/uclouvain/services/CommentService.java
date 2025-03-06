/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.content.Comment;

/**
 * Interface to describe comment service management.
 *    Some comments could be attached to an {@link org.dspace.content.Item} (0..N relation).
 *    A {@link org.dspace.uclouvain.content.Comment} is always related to one {@link org.dspace.content.Item}.
 *    This interface describes all methods necessary to manage item comments.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface CommentService {

    /**
     * Get all comments related to a specific item
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @return a list of comments related to the item.
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raised if current user doesn't have access to comment.
     * @throws Exception for any other exception
     */
    List<Comment> getComments(Context context, Item item) throws Exception;


    /**
     * Get a specific comment related to an item
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param commentID the comment ID to delete on this {@link org.dspace.content.Item}
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raise if current user doesn't have authorization to delete comment or item management.
     * @throws Exception for any other exception
     */
    Comment getComment(Context context, Item item, String commentID) throws Exception;

    /**
     * Add a comment on a parent item.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param person the author of the comment
     * @return the comment with updated data (mainly creationDate)
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raised if current user doesn't have authorization to add comment or item management.
     * @throws Exception for any other exception
     */
    Comment addComment(Context context, Item item, EPerson person, String content) throws Exception;

    /**
     * Add a comment on a parent item.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param authorName the author name of the comment
     * @return the comment with updated data (mainly creationDate)
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raised if current user doesn't have authorization to add comment or item management.
     * @throws Exception for any other exception
     */
    Comment addComment(Context context, Item item, String authorName, String content) throws Exception;

    /**
     * Add a system comment on an item.
     *    A system comment is comment where author is not related to any authority.
     *    We just use "system" as author name.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param content the comment content.
     * @return the created content
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raised if current user doesn't have authorization to add comment or item management.
     * @throws Exception for any other exception
     */
    Comment addSystemComment(Context context, Item item, String content) throws Exception;

    /**
     * Delete a comment.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param commentID the comment ID to delete on this {@link org.dspace.content.Item}
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raise if current user doesn't have authorization to delete comment or item management.
     * @throws ResourceNotFoundException raised if the comment cannot be found.
     */
    void deleteComment(Context context, Item item, String commentID)
            throws Exception;

    /**
     * Delete all comments related to an item.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raise if current user doesn't have authorization to delete comment or item management.
     */
    void deleteAllComment(Context context, Item item) throws SQLException, AuthorizeException, IOException;

    /**
     * Update the content of a comment related to an item.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param commentID the comment ID to modify
     * @param commentContent the new comment message to store
     * @throws SQLException raised when any database exceptions occurred
     * @throws AuthorizeException raise if current user doesn't have authorization to update comment or item management.
     */
    void updateComment(Context context, Item item, String commentID, String commentContent)
            throws Exception;

}
