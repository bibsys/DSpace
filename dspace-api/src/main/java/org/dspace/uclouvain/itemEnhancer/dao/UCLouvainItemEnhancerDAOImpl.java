/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.core.DBConnection;
import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.dspace.utils.DSpace;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DAO to interact with 'uclouvain_item_authority_metadata_enhancement' table.
 * This table is used as a queue for items that need to be updated using the
 * custom enhancement system.
 * The table contains 3 columns:
 * - The source_uuid, which is the item holding the proper metadata value.
 * - The target_uuid, which is the item to update the metadata of.
 * - The date_queued, which is giving a hint about the queue date.
 * 
 * @author: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerDAOImpl extends AbstractHibernateDAO<ItemToEnhance>
    implements UCLouvainItemEnhancerDAO {
    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerDAOImpl.class);

    @Autowired
    ConfigurationService configurationService;

    /**
     * Try to add a new entry to the database using a uuid pair (source + target).
     * If the pair is already present in the table, update its 'date_queued'.
     * 
     * @param context    The current DSpace context.
     * @param sourceUUID The UUID of the source item.
     * @param targetUUID The UUID of the target item.
     */
    @Override
    public void addOrUpdateItemToUpdate(Context context, UUID sourceUUID, UUID targetUUID) throws Exception {
        logger.debug("About to query the database to add an item for update (enhancement)");
        Session session = getHibernateSession();
        String sqlInsertOrUpdate;
        if ("org.h2.Driver".equals(configurationService.getProperty("db.driver"))) {
            // H2 doesn't support the INSERT OR UPDATE statement so let's use MERGE statement.
            // Update queued date for records already in the queue.
            sqlInsertOrUpdate = "MERGE INTO uclouvain_item_authority_metadata_enhancement as me"
                    + " KEY (source_uuid, target_uuid)"
                    + " VALUES (:source_uuid, :target_uuid, CURRENT_TIMESTAMP)";
        } else {
            sqlInsertOrUpdate =
                "INSERT INTO uclouvain_item_authority_metadata_enhancement (source_uuid, target_uuid, date_queued)"
                    + " VALUES (:source_uuid, :target_uuid, CURRENT_TIMESTAMP)"
                    + " ON CONFLICT (source_uuid, target_uuid) DO UPDATE"
                    + " SET date_queued = EXCLUDED.date_queued";
        }
        logger.info(
                "Adding an item to update to the database: source = " + sourceUUID + " target = " + targetUUID);
        NativeQuery<?> queryInsertOrUpdate = session.createNativeQuery(sqlInsertOrUpdate);
        // Fill the query parameters
        queryInsertOrUpdate.setParameter("source_uuid", sourceUUID);
        queryInsertOrUpdate.setParameter("target_uuid", targetUUID);
        queryInsertOrUpdate.executeUpdate();
    }

    /**
     * Retrieve all the entries from the table.
     * Those are converted in 'ItemToEnhance' objects to be used in the code.
     * Can return an empty list if no entries are found.
     * 
     * @param context The current DSpace context.
     * @return A list of 'ItemToEnhance' objects which can be empty.
     */
    @Override
    public List<ItemToEnhance> pollItemsToUpdate(Context context) throws Exception {
        Session session = getHibernateSession();
        String sql = "SELECT source_uuid, target_uuid, date_queued"
                + " FROM uclouvain_item_authority_metadata_enhancement"
                + " ORDER BY date_queued ASC";
        NativeQuery<ItemToEnhance> query = session.createNativeQuery(sql, ItemToEnhance.class);
        return query.getResultList();
    }

    /**
     * Delete all the entries in the table that are related to the given item uuid.
     * 
     * @param context The current DSpace context.
     * @param uuid The uuid of the item.
     * 
     * @return An integer to indicate the number of deleted entries.
     */
    @Override
    public Integer cleanTableEntriesForItem(Context context, UUID uuid) throws Exception {
        Session session = getHibernateSession();
        String sql = "DELETE FROM uclouvain_item_authority_metadata_enhancement"
             + " WHERE source_uuid = :source_uuid OR target_uuid = :target_uuid";
        NativeQuery<?> query = session.createNativeQuery(sql);
        query.setParameter("source_uuid", uuid);
        query.setParameter("target_uuid", uuid);
        return query.executeUpdate();
    }

    /**
     * Retrieves all the items that are linked to a source item.
     * To find those item, we browse all the available metadata values to find one those who have an authority
     * valid equal to the uuid of a source item.
     * The result list is parse into a lis of Item to have an easier access.
     * 
     * @param context The current DSpace context.
     * @param metadataField The metadata field to search for.
     * @param authority The authority we are searching for.
     * @return A list of all related items which have at least one metadata referencing the source item.
     */
    @Override
    public List<Item> getAuthorityLinkedItem(
        Context context, MetadataField metadataField, String authority
    ) throws Exception {
        Session session = getHibernateSession();
        String sql = "SELECT result_item.*"
            + " FROM"
            + " (SELECT mv.dspace_object_id"
                + " FROM metadatavalue as mv"
                + " JOIN metadatafieldregistry as mf on mv.metadata_field_id = mf.metadata_field_id"
                + " JOIN metadataschemaregistry as ms on mf.metadata_schema_id = ms.metadata_schema_id"
                + " WHERE ms.short_id = :schema and mf.element = :element"
                + (
                    metadataField.getQualifier() != null
                        ? (" and mf.qualifier = '" + metadataField.getQualifier().toString() + "'")
                        : ""
                )
                + " and mv.authority = :authority"
                + " GROUP BY mv.dspace_object_id) as result_uuids,"
            + " (SELECT * FROM item) as result_item"
            + " WHERE result_uuids.dspace_object_id = result_item.uuid";

        NativeQuery<Item> query = session.createNativeQuery(sql, Item.class);
        query.setParameter("schema", metadataField.getMetadataSchema().getName());
        query.setParameter("element", metadataField.getElement());
        query.setParameter("authority", authority);

        return query.getResultList();
    }


    /**
     * Returns the Hibernate Session currently opened.
     *
     * @return The current Session.
     * @throws SQLException
     */
    private Session getHibernateSession() throws SQLException {
        @SuppressWarnings("rawtypes")
        DBConnection dbConnection = new DSpace().getServiceManager().getServiceByName(null, DBConnection.class);
        return ((Session) dbConnection.getSession());
    }
}
