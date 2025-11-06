/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataField_;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataValue object.
 * This class is responsible for all database calls for the MetadataValue object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataValueDAOImpl extends AbstractHibernateDAO<MetadataValue> implements MetadataValueDAO {
    protected MetadataValueDAOImpl() {
        super();
    }


    @Override
    public List<MetadataValue> findByField(Context context, MetadataField metadataField) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, MetadataValue.class);
        Root<MetadataValue> metadataValueRoot = criteriaQuery.from(MetadataValue.class);
        Join<MetadataValue, MetadataField> join = metadataValueRoot.join("metadataField");
        criteriaQuery.select(metadataValueRoot);
        criteriaQuery.where(criteriaBuilder.equal(join.get(MetadataField_.id), metadataField.getID()));

        return list(context, criteriaQuery, false, MetadataValue.class, -1, -1);
    }

    @Override
    public List<MetadataValue> findByAuthority(Context context, String authority) throws SQLException {
        String queryString = "SELECT m from MetadataValue m WHERE m.authority = :metadata_authority";
        Query query = createQuery(context, queryString);
        query.setParameter("metadata_authority", authority);
        return list(query);
    }

    /**
     * Get all the items that are supposedly connected to the given metadata values.
     * A match is processed using a 'OR' logic. If an item has at least one corresponding value from the given fields,
     * it is considered matching and it will be returned.
     * 
     * @param context The current DSpace context.
     * @param fieldsValues A map of each field to use to retrieve matching items.
     * @param keepAuthorityLinked Whenever to get authority linked metadata values or not.
     * False means only metadata values with no authority.
     */
    @Override
    public List<Pair<DSpaceObject, Integer>> findByFieldAndValue(
        Context context, Map<Integer, String> fieldsValues, boolean keepAuthorityLinked
    ) throws SQLException, IllegalArgumentException {
        if (fieldsValues.isEmpty()) {
            throw new IllegalArgumentException("fieldsValues should have at least one value");
        }
        String metadataCondition = fieldsValues.entrySet()
            .stream()
            .map(entry -> String.format(
                    "(mv.metadataField.id = %d and mv.value = '%s')",
                    entry.getKey(), entry.getValue()
                )
            ).collect(Collectors.joining(" or "));
        String metadataAuthority = keepAuthorityLinked ? "" : " and mv.authority is NULL";
        String query = "SELECT DISTINCT mv.dSpaceObject, mv.place FROM MetadataValue mv WHERE (%s)%s".formatted(
            metadataCondition,
            metadataAuthority
        );
        // Use Tuples to return a List of Pairs.
        return getHibernateSession(context)
            .createQuery(query, Tuple.class)
            .getResultList()
            .stream()
            .map(t -> Pair.of(t.get(0, DSpaceObject.class), t.get(1, Integer.class)))
            .collect(Collectors.toList());
    }

    @Override
    public Iterator<MetadataValue> findItemValuesByFieldAndValue(Context context,
                                                                 MetadataField metadataField, String value)
            throws SQLException {
        String queryString = "SELECT m from MetadataValue m " +
                "join Item i on m.dSpaceObject = i where m.metadataField.id = :metadata_field_id " +
                "and m.value = :text_value";
        Query query = createQuery(context, queryString);
        query.setParameter("metadata_field_id", metadataField.getID());
        query.setParameter("text_value", value);
        return iterate(query);
    }

    @Override
    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException {
        String queryString = "SELECT m FROM MetadataValue m JOIN m.metadataField f " +
            "WHERE m.value like concat('%', concat(:searchString,'%')) ORDER BY m.id ASC";

        Query query = createQuery(context, queryString);
        query.setParameter("searchString", value);

        return iterate(query);
    }

    @Override
    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException {
        String queryString = "delete from MetadataValue where metadataField= :metadataField";
        Query query = createQuery(context, queryString);
        query.setParameter("metadataField", metadataField);
        query.executeUpdate();
    }

    @Override
    public MetadataValue getMinimum(Context context, int metadataFieldId)
        throws SQLException {
        String queryString = "SELECT m FROM MetadataValue m JOIN FETCH m.metadataField WHERE m.metadataField.id = " +
            ":metadata_field_id ORDER BY value";
        Query query = createQuery(context, queryString);
        query.setParameter("metadata_field_id", metadataFieldId);
        query.setMaxResults(1);
        return (MetadataValue) query.getSingleResult();
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM MetadataValue"));
    }

}
