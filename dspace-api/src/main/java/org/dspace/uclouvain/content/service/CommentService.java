/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.content.Comment;

/**
 * Service that allows management of {@link org.dspace.uclouvain.content.Comment} related to
 * {@link org.dspace.content.Item}
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public interface CommentService {

    /**
     * Allows creating a new {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item}
     *
     * @param context the application context.
     * @param owner the {@link org.dspace.content.Item} owner of the comment.
     * @param author the author (aka writer) of the comment.
     * @param content the comment content.
     * @return the created comment.
     * @throws SQLException if any database errors occurred.
     */
    Comment create(Context context, Item owner, EPerson author, String content) throws SQLException;

    /**
     * Allows creating a new {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item}
     * but not related to an authority {@link org.dspace.eperson.EPerson}.
     *
     * @param context the application context.
     * @param owner the {@link org.dspace.content.Item} owner of the comment.
     * @param authorName the author (aka writer) name of the comment.
     * @param content the comment content.
     * @return the created comment.
     * @throws SQLException if any database errors occurred.
     */
    Comment create(Context context, Item owner, String authorName, String content) throws SQLException;

    /**
     * Allow creating a new {@link org.dspace.uclouvain.content.Comment} not linked to a specific authority but to the
     * system.
     *
     * @param context the application context.
     * @param owner the {@link org.dspace.content.Item} owner of the comment.
     * @param content the comment content.
     * @return the created comment.
     * @throws SQLException if any database errors occurred.
     */
    Comment createSystemComment(Context context, Item owner, String content) throws SQLException;

    /**
     * Update the content of a {@link org.dspace.uclouvain.content.Comment}.
     *
     * @param context the application context.
     * @param comment the {@link org.dspace.uclouvain.content.Comment} to update.
     * @param content the comment content.
     */
    void updateCommentContent(Context context, Comment comment, String content) throws SQLException;

    /**
     * Allows deleting a single {@link org.dspace.uclouvain.content.Comment}
     *
     * @param context the application context.
     * @param comment the comment to delete.
     * @throws SQLException if any database errors occurred.
     */
    void delete(Context context, Comment comment) throws SQLException;

    /**
     * Allow deleting all {@link org.dspace.uclouvain.content.Comment} related to a specific
     * {@link org.dspace.content.Item}
     *
     * @param context the application context.
     * @param item the related {@link org.dspace.content.Item}
     * @throws SQLException if any database errors occurred.
     */
    void deleteAllItemComments(Context context, Item item) throws SQLException;

    /**
     * Get a specific {@link org.dspace.uclouvain.content.Comment} stored into the database.

     * @param context the application context.
     * @param id the comment ID to search.
     * @return the corresponding {@link org.dspace.uclouvain.content.Comment} or null if no comment could be found.
     * @throws SQLException if any database errors occurred.
     */
    Comment find(Context context, UUID id) throws SQLException;

    /**
     * Get all {@link org.dspace.uclouvain.content.Comment} stored into the database. Use with caution because it could
     * return a very large list.
     *
     * @param context the application context.
     * @return a list of {@link org.dspace.uclouvain.content.Comment} stored into the database.
     * @throws SQLException if any database errors occurred.
     */
    List<Comment> findAll(Context context) throws SQLException;

    /**
     * Get all {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item} sorted on
     * comment creation date.
     *
     * @param context the application context.
     * @param item the related {@link org.dspace.content.Item}
     * @param asc is the result should be sort ascending or descending?
     * @return a list of {@link org.dspace.uclouvain.content.Comment} stored into the database.
     * @throws SQLException if any database errors occurred.
     */
    List<Comment> findByItem(Context context, Item item, boolean asc) throws SQLException;

    /**
     * Get all {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item}.
     *
     * @param context the application context.
     * @param item the related {@link org.dspace.content.Item}
     * @return a list of {@link org.dspace.uclouvain.content.Comment} stored into the database.
     * @throws SQLException if any database errors occurred.
     */
    List<Comment> findByItem(Context context, Item item) throws SQLException;

    /**
     * Count the number of {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item}.
     *
     * @param context the application context.
     * @param item the related {@link org.dspace.content.Item}
     * @return the number of comments related to the specified item.
     * @throws SQLException if any database errors occurred.
     */
    int countCommentByItem(Context context, Item item) throws SQLException;

    /**
     * Set comment created date using a custom timestamp
     * It could be useful for legacy comment, keeping the legacy original comment creation timestamp
     *
     * @param context the application context
     * @param comment the comment to update
     * @param timestamp the creation timestamp to set
     */
    void forceCreatedDate(Context context, Comment comment, Date timestamp) throws SQLException;
}
