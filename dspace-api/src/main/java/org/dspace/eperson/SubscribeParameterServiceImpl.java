/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.dspace.eperson.dao.SubscriptionParameterDAO;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.eperson.service.SubscriptionParameterService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

/**
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubscribeParameterServiceImpl implements SubscriptionParameterService {
    /**
     * log4j logger
     */
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(SubscribeParameterServiceImpl.class);

    @Autowired(required = true)
    protected SubscriptionParameterDAO subscriptionParameterDAO;


    protected SubscribeParameterServiceImpl() {

    }

    @Override
    public List<SubscriptionParameter> findAll(Context context) throws SQLException {
        return subscriptionParameterDAO.findAll(context, SubscriptionParameter.class);
    }

    @Override
    public SubscriptionParameter add(Context context, String name, String value, Subscription subscription) throws SQLException, AuthorizeException {
        SubscriptionParameter subscriptionParameter =  subscriptionParameterDAO.create(context, new SubscriptionParameter());
        subscriptionParameter.setName(name);
        subscriptionParameter.setSubscription(subscription);
        subscriptionParameter.setValue(value);
        return subscriptionParameter;
    }
    @Override
    public SubscriptionParameter edit(Context context,Integer id,  String name, String value, Subscription subscription) throws SQLException, AuthorizeException {
        SubscriptionParameter subscriptionParameter = subscriptionParameterDAO.findByID(context, SubscriptionParameter.class, id);
        subscriptionParameter.setId(id);
        subscriptionParameter.setName(name);
        subscriptionParameter.setSubscription(subscription);
        subscriptionParameter.setValue(value);
        subscriptionParameterDAO.save(context, subscriptionParameter);
        return subscriptionParameter;


    }

    @Override
    public SubscriptionParameter findById(Context context, int id) throws SQLException {
        return subscriptionParameterDAO.findByID(context, SubscriptionParameter.class, id);
    }

    @Override
    public void deleteSubscriptionParameter(Context context, Integer id) throws SQLException, AuthorizeException {
        SubscriptionParameter subscriptionParameter = subscriptionParameterDAO.findByID(context, SubscriptionParameter.class, id);
        if (subscriptionParameter != null) {
            subscriptionParameterDAO.delete(context, subscriptionParameter);
        } else {
            throw new SQLException("Subscription parameter with id" + id + "do not exists");
        }

    }
}