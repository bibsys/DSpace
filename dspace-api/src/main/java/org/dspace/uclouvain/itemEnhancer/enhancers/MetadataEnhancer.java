/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An abstract representation of an 'Enhancer'.
 * An 'Enhancer" is a class called when an object is modified to potentially update other linked objects.
 * 
 * Every 'Enhancer' has to be defined for a specific action and entity-type.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public abstract class MetadataEnhancer<T> {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    @Autowired
    protected ItemService itemService;
    @Autowired
    protected UCLouvainItemEnhancerService itemEnhancerService;

    public abstract boolean enhance(Context context, T object) throws Exception;
    public abstract String getSupportedAction();
    public abstract String getSupportedEntityType();

    protected static final Logger logger = LogManager.getLogger(MetadataEnhancer.class);

    public boolean supports(String entityType, String action) {
        return Objects.equals(action, getSupportedAction())
            && Objects.equals(entityType, getSupportedEntityType());
    }
}
