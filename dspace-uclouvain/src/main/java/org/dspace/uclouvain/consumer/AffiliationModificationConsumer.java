package org.dspace.uclouvain.consumer;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;

/**
 * Consumer which is used to trigger an update of the {@link org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestServiceImpl}'s 'affiliationsEntities' property when an OrgUnit is modified.
 * 
 * @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class AffiliationModificationConsumer implements Consumer {
    private UCLouvainAffiliationEntityRestService affiliationEntityRestService = UCLouvainServiceFactory.getInstance().getAffiliationEntityRestService();
    private EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    private Logger logger = LogManager.getLogger(AffiliationModificationConsumer.class);

    @Override
    public void initialize() {}

    @Override
    public void consume(Context context, Event event) throws SQLException {
        try {
            Item item = (Item) event.getSubject(context);
            EntityType targetEntityType = this.entityTypeService.findByEntityType(context, "OrgUnit");
            if (targetEntityType != null && this.entityTypeService.findByItem(context, item) == targetEntityType) {
                this.affiliationEntityRestService.updateAffiliationEntities();
            }
        } catch (Exception e) {
            this.logger.error("Error while updating affiliation entities via the consumer.", e);
        }
    }

    @Override
    public void finish(Context context) throws Exception {}

    @Override
    public void end(Context context) throws Exception {}
}
