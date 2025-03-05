/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.uclouvain.content.Comment;

/**
 * Database Access Object interface class for the {@link org.dspace.uclouvain.content.Comment} object.
 * The implementation of this class is responsible for all database calls for the Comment object and is autowired by
 * spring framework.
 *
 * !!!This class should only be accessed from a single service and should never be exposed outside the API
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public interface CommentDAO extends GenericDAO<Comment> {

    /**
     * Search about {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item}
     *
     * @param context the application context.
     * @param item the item owner of the comment.
     * @return the list of comments related to this item.
     * @throws SQLException if any database errors occurred.
     */
    List<Comment> findByItem(Context context, Item item) throws SQLException;

    /**
     * Count the number of {@link org.dspace.uclouvain.content.Comment} related to an {@link org.dspace.content.Item}
     *
     * @param context the application context.
     * @param item the item owner of the comment.
     * @return the count of comments related to this item.
     * @throws SQLException if any database errors occurred.
     */
    int countCommentsForItem(Context context, Item item) throws SQLException;
}
