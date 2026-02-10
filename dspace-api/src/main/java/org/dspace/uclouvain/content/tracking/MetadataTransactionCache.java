/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.tracking;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.dspace.core.Context;
import org.springframework.stereotype.Component;


/**
 * Implementation of a specific cache used to store {@link MetadataValueSnapshot} during
 * a DSpace transaction. When the transaction (in real: {@link Context}) is closed/terminated, the corresponding cache
 * is automatically deleted
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class MetadataTransactionCache {

    // WeakHashMap : the key (Context) is automatically deleted when context is revoked (garbage collected)
    // We use `Collections.synchronizedMap` for security between Thread (because Tomcat could reuse a thread)
    private final Map<Context, Map<UUID, List<MetadataValueSnapshot>>> internalCache =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Allow to store a list of {@link MetadataValueSnapshot} into a temporary
     * cache valid only when context still exist (only for a DSpace transaction). When the context is completed (and
     * garbage collected), the cache is automatically destroyed.
     * @param context The DSpace application context (used as key)
     * @param dsoID The {@link org.dspace.content.DSpaceObject} ID corresponding to metadata snapshot (used a key)
     * @param snapshot the list of {@link MetadataValueSnapshot} to store.
     */
    public synchronized void put(Context context, UUID dsoID, List<MetadataValueSnapshot> snapshot) {
        if (context.isMetadataTrackingEnabled()) {
            internalCache.computeIfAbsent(context, k -> new HashMap<>()).put(dsoID, snapshot);
        }
    }

    /**
     * Retrieve a list of {@link MetadataValueSnapshot} previously stored into the cache
     * @param context The DSpace application context (used as key)
     * @param dsoID The {@link org.dspace.content.DSpaceObject} ID corresponding to metadata snapshot (used a key)
     * @return the desired list of {@link MetadataValueSnapshot} or null if no corresponding entries are found into the
     *         cache.
     */
    public synchronized List<MetadataValueSnapshot> get(Context context, UUID dsoID) {
        if (context.isMetadataTrackingEnabled()) {
            Map<UUID, List<MetadataValueSnapshot>> ctxMap = internalCache.get(context);
            return (ctxMap != null)
                ? ctxMap.get(dsoID)
                : null;
        } else {
            return null;
        }
    }

    /**
     * Force a revocation of stored metadata even if {@link Context} is still alive
     * @param context The DSpace application context (used as key)
     * @param dsoID The {@link org.dspace.content.DSpaceObject} ID corresponding to metadata snapshot (used a key)
     */
    public synchronized void remove(Context context, UUID dsoID) {
        Optional.ofNullable(internalCache.get(context))
                .ifPresent(ctxMap -> ctxMap.remove(dsoID));
    }
}
