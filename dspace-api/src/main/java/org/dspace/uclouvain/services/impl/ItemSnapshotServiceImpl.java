/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.apicatalog.jsonld.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.dao.ItemSnapshotDAO;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.ItemSnapshotContentSerializer;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.explainer.DiffExplainerFactory;
import org.dspace.uclouvain.content.snapshot.diff.formats.OutputFormat;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ItemSnapshotService}, used to deal with {@link ItemSnapshot}
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Service
public class ItemSnapshotServiceImpl implements ItemSnapshotService {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemSnapshotServiceImpl.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemSnapshotDAO itemSnapshotDAO;
    @Autowired
    private AccessStatusService accessStatusService;
    @Autowired
    private ItemSnapshotContentSerializer snapshotSerializer;
    @Autowired
    private DiffExplainerFactory diffExplainerFactory;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final List<String> trackedMetadata = Stream
        .of(configService.getArrayProperty("item-snapshot.tracked-metadata", new String[] {}))
        .toList();

    // IMPLEMENTED METHODS =============================================================================================
    @Override
    public ItemSnapshot get(Context context, UUID id, boolean deserialize) throws SQLException {
        ItemSnapshot snapshot = itemSnapshotDAO.findByID(context, ItemSnapshot.class, id);
        if (snapshot == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_item_snapshot", "not_found,item_id=" + id));
            }
            return null;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_item_snapshot", "item_id=" + id));
            }
            if (deserialize) {
                try {
                    snapshotSerializer.deserialize(snapshot);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to deserialize ItemSnapshot#" + id, e);
                }
            }
            return snapshot;
        }
    }
    @Override
    public ItemSnapshot get(Context context, UUID id) throws SQLException {
        return get(context, id, true);
    }

    @Override
    public ItemSnapshot takeSnapshot(Context context, UUID id) throws SQLException {
        Item item = itemService.find(context, id);
        if (item == null) {
            throw new IllegalArgumentException("Item#" + id + " doesn't exist");
        }
        return this.takeSnapshot(context, item);
    }
    @Override
    public ItemSnapshot takeSnapshot(Context context, Item item) throws SQLException {
        ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setItem(item);
        snapshot.setTimestamp(new Date());
        snapshot.setSnapshotElements(this.buildSnapshotElements(context, item));
        return snapshot;
    }

    @Override
    public ItemSnapshotDiff compareSnapshot(ItemSnapshot snapshot1, ItemSnapshot snapshot2)
        throws IllegalArgumentException {
        if (snapshot1 == null || snapshot2 == null) {
            throw new IllegalArgumentException("Snapshot cannot be null");
        }
        // DEV NOTE: We use `getItem().getID()` instead of `getId()` because the snapshot could be not yet persisted
        if (!Objects.equals(snapshot1.getItem().getID(), snapshot2.getItem().getID())) {
            throw new IllegalArgumentException(String.format(
                    "We can only compare snapshots for the same related item :: %s<>%s",
                    snapshot1.getId(), snapshot2.getId()
            ));
        }
        ItemSnapshotDiff diff = new ItemSnapshotDiff(snapshot1.getItem());

        // For better performance, we will index the second snapshot using a single key (path + class)
        Map<String, SnapshotElement> mapSnapshot2 = snapshot2
            .getSnapshotElements().stream()
            .collect(Collectors.toMap(
                this::getSnapshotElementKey,
                element -> element,
                (existing, replacement) -> existing // For potential doubles...
            ));
        // Loop on snapshot1 elements to find update/remove elements
        //   If current read element key isn't present into mapSnapshot2: This is a "remove"
        //   If element path exists in both snapshots, but aren't equals: This is an "update"
        for (SnapshotElement element1 : snapshot1.getSnapshotElements()) {
            String key = getSnapshotElementKey(element1);
            SnapshotElement element2 = mapSnapshot2.remove(key);
            if (element2 == null || !element1.equals(element2)) {
                diff.addChange(element1, element2);
            }
        }
        // At the end, elements not removed from the constructed map and "added" element
        for (SnapshotElement element : mapSnapshot2.values()) {
            diff.addChange(null, element);
        }
        return diff;
    }
    private String getSnapshotElementKey(SnapshotElement snapshotElement) {
        return snapshotElement.getPath() + "__" + snapshotElement.getClass().getName();
    }

    @Override
    public ItemSnapshotDiff detectChanges(Context context, UUID id) throws Exception {
        Item item = itemService.find(context, id);
        if (item == null) {
            throw new IllegalArgumentException("Item#" + id + " doesn't exist");
        }
        return this.detectChanges(context, item);
    }
    @Override
    public ItemSnapshotDiff detectChanges(Context context, Item item) throws Exception {
        ItemSnapshot snapshot1 = get(context, item.getID());
        if (snapshot1 == null) {
            throw new IllegalArgumentException("Unable to retrieve snapshot for Item#" + item.getID());
        }
        return compareSnapshot(snapshot1, takeSnapshot(context, item));
    }

    @Override
    public String explainChanges(ItemSnapshotDiff changes, OutputFormat format) {
        return changes.getChanges().stream()
            .map(change -> diffExplainerFactory.explain(change.getLeft(), change.getRight(), format))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining());
    }


    @Override
    public void store(Context context, ItemSnapshot snapshot) throws Exception {
        if (snapshot.getItem() == null) {
            throw new IllegalArgumentException("Snapshot must be related to an item");
        }

        // Persist changes into database
        //    If the database doesn't yet store a snapshot for the related item, just "create" the snapshot
        //    If the database already stored a snapshot, we need to retrieve it, update it and "save" it
        if (snapshot.getId() == null) {
            snapshot.setId(snapshot.getItem().getID());
        }
        ItemSnapshot existingSnapshot = itemSnapshotDAO.findByID(context, ItemSnapshot.class, snapshot.getId());
        if (existingSnapshot == null) {
            snapshot.setContent(snapshotSerializer.serialize(snapshot));
            itemSnapshotDAO.create(context, snapshot);
        } else {
            existingSnapshot.setContent(snapshotSerializer.serialize(snapshot));
            existingSnapshot.setTimestamp(snapshot.getTimestamp());
            itemSnapshotDAO.save(context, existingSnapshot);
        }

    }

    // PRIVATE METHODS =================================================================================================
    /**
     * This method analyze an item to extract any snapshot element useful to create the snapshot
     * @param context the application context
     * @param item the item to analyze
     * @return a list of {@link SnapshotElement} regarding this object
     * @throws SQLException if any database exception occurred
     */
    private List<SnapshotElement> buildSnapshotElements(Context context, Item item) throws SQLException {
        return Stream.concat(
            buildSnapshotMetadataElements(item).stream(),
            buildSnapshotFileElements(context, item).stream()
        ).collect(Collectors.toList());
    }
    private List<SnapshotElement> buildSnapshotMetadataElements(Item item) {
        List<SnapshotElement> elements = new ArrayList<>();
        for (String trackedField : trackedMetadata) {
            List<MetadataValue> mdValues = itemService.getMetadataByMetadataString(item, trackedField);
            for (MetadataValue md : mdValues) {
                String fieldPath = md.getMetadataField().toString('.');
                String mdPath = String.format("%s[%d]", fieldPath, md.getPlace());
                elements.add(new MetadataSnapshotElement(mdPath, md.getValue()));
            }
        }
        return elements;
    }
    private List<SnapshotElement> buildSnapshotFileElements(Context context, Item item) throws SQLException {
        List<SnapshotElement> elements = new ArrayList<>();
        List<Bundle> bundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        for (Bundle bundle : bundles) {
            for (Bitstream b : bundle.getBitstreams()) {
                elements.add(new FileSnapshotElement(
                    b.getID(),
                    b.getName(),
                    b.getChecksumAlgorithm() + "#" + b.getChecksum(),
                    accessStatusService.getBitstreamAccessStatus(context, b)
                ));
            }
        }
        return elements;
    }
}
