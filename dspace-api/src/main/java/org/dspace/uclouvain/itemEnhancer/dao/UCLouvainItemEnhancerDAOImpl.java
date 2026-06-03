/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.dao;

// import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.Tuple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.core.DBConnection;
import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.dspace.utils.DSpace;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DAO to interact with 'uclouvain_item_authority_metadata_enhancement' table.
 * 
 * @author: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerDAOImpl extends AbstractHibernateDAO<ItemToEnhance>
    implements UCLouvainItemEnhancerDAO {
    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerDAOImpl.class);

    @Autowired
    ConfigurationService configurationService;

    /**
     * Try to add a new entry to the database using a given item uuid and its entity-type.
     * If the item is already present in the table, update its 'date_queued'.
     * 
     * @param context The current DSpace application context.
     * @param uuid The UUID of the item to trigger an enhancement from.
     * @param entityType The entity-type of the item.
     */
    @Override
    public void addOrUpdateItemToUpdate(Context context, UUID uuid, String entityType) throws SQLException {
        logger.debug("About to query the database to add an item for enhancement");
        Session session = getHibernateSession();

        // DEV_NOTE: Usage of hibernate instead of SQL for better readability/stability.
        ItemToEnhance existingEntry = session.get(ItemToEnhance.class, uuid);
        if (existingEntry == null) {
            ItemToEnhance newItem = new ItemToEnhance();
            newItem.setItemUUID(uuid);
            newItem.setEntityType(entityType);
            newItem.setDateQueued(new Date());
            session.persist(newItem);
        } else {
            existingEntry.setDateQueued(new Date());
            session.merge(existingEntry);
        }
    }

    /**
     * Retrieve all the entries from the table until the given limit is reached.
     * Those are converted in 'ItemToEnhance' objects to be used in the code.
     * Can return an empty list if no entries are found.
     * 
     * @param context The current DSpace application context.
     * @param limit Limit the number of item to retrieve. -1 for unlimited number of result.
     * @return A possibly empty list of 'ItemToEnhance' objects.
     */
    @Override
    public List<ItemToEnhance> getItemsToEnhance(Context context, Integer limit) throws SQLException {
        Session session = getHibernateSession();
        String hql = "FROM ItemToEnhance ORDER BY dateQueued ASC";
        Query<ItemToEnhance> query = session.createQuery(hql, ItemToEnhance.class);
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    /**
     * Retrieve the total amount of entries for the database table.
     * 
     * @param context The current DSpace application context.
     * @return The total number of entries for the table so the total number of item to enhance.
     */
    @Override
    public Integer countItemsToEnhance(Context context) throws SQLException {
        Session session = getHibernateSession();
        String hql = "SELECT count(i) FROM ItemToEnhance i";
        Query<Long> query = session.createQuery(hql, Long.class);
        return query.getSingleResult().intValue();
    }

    /**
     * Delete the entry in the table that is related to the given item uuid.
     * 
     * @param context The current DSpace application context.
     * @param uuid The uuid of the item.
     * 
     * @return An integer to indicate the number of deleted entries (should be 1).
     */
    @Override
    public Integer cleanTableEntriesForItem(Context context, UUID uuid) throws SQLException {
        Session session = getHibernateSession();
        ItemToEnhance existing = session.get(ItemToEnhance.class, uuid);
        if (existing != null) {
            session.remove(existing);
            return 1;
        }
        return 0;
    }

    /**
     * Delete all the entries in the table that have a date evaluated between the given 'startDate' and 'endDate'.
     * 
     * @param context   The current DSpace application context.
     * @param startDate The start date to delete the correct entries.
     * @param endDate   The end date to delete the correct entries.
     * @return An integer to indicate the number of deleted entries.
     */
    @Override
    public Integer cleanTableEntries(Context context, Date startDate, Date endDate) throws SQLException {
        Session session = getHibernateSession();
        String hql = "DELETE FROM ItemToEnhance WHERE dateQueued BETWEEN :start_date AND :end_date";
        Query<?> query = session.createQuery(hql, null);
        query.setParameter("start_date", startDate);
        query.setParameter("end_date", endDate);
        return query.executeUpdate();
    }

    /**
     * Retrieves all the items that are linked to a source item and the place of the metadata value linking them.
     * To find those item, we browse all the available metadata values to find one those who have an authority
     * valid equal to the uuid of a source item.
     * 
     * @param context The current DSpace context.
     * @param metadataField The metadata field to search for.
     * @param authority The authority we are searching for.
     * @return A list of all related items which have at least one metadata referencing the source item.
     */
    @Override
    public List<Pair<Item, Integer>> getAuthorityLinkedItem(
        Context context, MetadataField metadataField, String authority
    ) throws SQLException {
        Session session = getHibernateSession();
        String hql = "SELECT DISTINCT mv.dSpaceObject, mv.place FROM MetadataValue mv " +
                    "WHERE mv.metadataField = :metadataField " +
                    "AND mv.authority = :authority";

        // Use the 'Tuple' type to convert into a pair later.
        Query<Tuple> query = session.createQuery(hql, Tuple.class);
        query.setParameter("metadataField", metadataField);
        query.setParameter("authority", authority);

        // Map the tuple stream into a list of pairs.
        return query.getResultList()
            .stream()
            .map(t -> Pair.of((Item) t.get(0, DSpaceObject.class), t.get(1, Integer.class)))
            .collect(Collectors.toList());
    }

    /**
     * Returns the Hibernate Session currently opened.
     *
     * @return The current Session.
     */
    private Session getHibernateSession() throws SQLException {
        @SuppressWarnings("rawtypes")
        DBConnection dbConnection = new DSpace().getServiceManager().getServiceByName(null, DBConnection.class);
        return ((Session) dbConnection.getSession());
    }
}
