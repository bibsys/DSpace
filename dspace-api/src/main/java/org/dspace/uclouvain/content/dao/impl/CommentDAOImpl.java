/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.content.dao.CommentDAO;

@SuppressWarnings("unchecked")
public class CommentDAOImpl extends AbstractHibernateDAO<Comment> implements CommentDAO {

    @Override
    public List<Comment> findByItem(Context context, Item item, boolean asc) throws SQLException {
        String direction = (asc) ? "ASC" : "DESC";
        Query query = createQuery(context,
                "SELECT c " +
                "FROM Comment c " +
                "WHERE c.owner = :owner " +
                "ORDER BY c.creationDate " + direction
        );
        query.setParameter("owner", item);
        return query.getResultList();
    }

    @Override
    public int countCommentsForItem(Context context, Item item) throws SQLException {
        Query query = createQuery(context, "SELECT count(c) FROM Comment c WHERE c.owner = :owner");
        query.setParameter("owner", item);
        return count(query);
    }
}
