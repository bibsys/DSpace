/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the MetadataValue object.
 * The implementation of this class is responsible for all database calls for the MetadataValue object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataValueDAO extends GenericDAO<MetadataValue> {

    public List<MetadataValue> findByField(Context context, MetadataField fieldId) throws SQLException;

    public List<MetadataValue> findByAuthority(Context context, String authority) throws SQLException;

    /**
     * Get all the items that are supposedly connected to the given metadata values.
     * A match is processed using a 'OR' logic. If an item has at least one corresponding value from the given fields,
     * it is considered matching and it will be returned.
     * 
     * @param context The current DSpace context.
     * @param fieldsValues A map of each field to use to retrieve matching items.
     * @param keepAuthorityLinked Whenever to keep authority linked metadata values or not.
     * False means only metadata values with no authority.
     */
    public List<Pair<DSpaceObject, Integer>> findByFieldAndValue(
        Context context, Map<Integer, String> fieldsValues, boolean keepAuthorityLinked
    ) throws SQLException, IllegalArgumentException;

    public Iterator<MetadataValue> findItemValuesByFieldAndValue(Context context,
                                                                 MetadataField metadataField, String value)
            throws SQLException;

    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException;

    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException;

    public MetadataValue getMinimum(Context context, int metadataFieldId)
        throws SQLException;

    int countRows(Context context) throws SQLException;

}
